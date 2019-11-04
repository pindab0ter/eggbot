package nl.pindab0ter.eggbot.commands

import com.auxbrain.ei.EggInc
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.commands.categories.AdminCategory
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.CoopFarmers
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.network.AuxBrain
import nl.pindab0ter.eggbot.utilities.*
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.roundToInt

object RollCall : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "roll-call"
        arguments = "<contract id> [overwrite]"
        aliases = arrayOf("rc", "rollcall")
        help = "Create a co-op roll call for the specified contract id."
        category = AdminCategory
        guildOnly = false
    }

    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        (checkPrerequisites(
            event,
            adminRequired = true,
            minArguments = 1,
            maxArguments = 2
        ) as? PrerequisitesCheckResult.Failure)?.message?.let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        if (Config.devMode) transaction {
            Coops.deleteAll()
            CoopFarmers.deleteAll()
        }

        val contractInfo: EggInc.Contract? = AuxBrain.getPeriodicals()?.contracts?.contractsList?.find {
            it.id == event.arguments.first()
        }

        val force: Boolean = event.arguments.getOrNull(1)?.equals("overwrite") == true

        if (contractInfo == null) "No active contract found with id `${event.arguments.first()}`".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        if (!contractInfo.coopAllowed) "Co-op is not allowed for this contract".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        transaction {
            if (!Coops.select { Coops.contract eq contractInfo.id }.empty()) {
                if (force) {
                    transaction {
                        Coops.deleteWhere { Coops.contract eq contractInfo.id }
                    }
                } else "Co-ops are already generated for contract `${contractInfo.id}`. Add `overwrite` to override.".let {
                    event.replyWarning(it)
                    log.debug { it }
                    return@transaction
                }
            }

            val farmers = transaction { Farmer.all().sortedByDescending { it.earningsBonus }.toList() }
            val coops: List<Coop> = PaddingDistribution.createRollCall(farmers, contractInfo)
            val longestFarmerName = farmers.maxBy { it.inGameName.length }!!.inGameName
            val longestEarningsBonus = farmers
                .maxBy { it.earningsBonus.formatIllions(true).length }!!.earningsBonus.formatIllions(true)


            event.reply(StringBuilder("Co-ops generated for `${contractInfo.id}`:").appendln().apply {
                append("```")
                coops.forEach { coop ->
                    append(coop.name)
                    appendPaddingCharacters(coop.name, coops.map { it.name })
                    append(" (")
                    appendPaddingCharacters(coop.farmers.count(), coops.map { it.farmers.count() })
                    append(coop.farmers.count())
                    append("/${contractInfo.maxCoopSize} members): ")
                    appendPaddingCharacters(
                        coop.activeEarningsBonus.formatIllions(true),
                        coops.map { it.activeEarningsBonus.formatIllions(true) })
                    append(coop.activeEarningsBonus.formatIllions(true) + " %")
                    appendln()
                }
                append("```")
            }.toString())

            coops.joinToString("\u000C") { coop ->
                StringBuilder("\u200B\nCo-op `${coop.name}`:").appendln().apply {
                    append("```")
                    coop.farmers.forEach { farmer ->
                        append(farmer.inGameName)
                        append(": ")
                        appendPaddingCharacters(
                            farmer.inGameName,
                            longestFarmerName
                        )
                        appendPaddingCharacters(
                            farmer.earningsBonus.formatIllions(true),
                            longestEarningsBonus
                        )
                        append(farmer.earningsBonus.formatIllions(true) + " %")
                        if (!farmer.isActive) append(" (Inactive)")
                        appendln()
                    }
                    append("```")
                }.toString()
            }.splitMessage(separator = '\u000C').forEach { message ->
                event.reply(message)
            }
        }
    }

    object PaddingDistribution {
        private const val FILL_PERCENTAGE = 0.8

        private fun createCoops(
            farmers: List<Farmer>,
            contract: EggInc.Contract,
            preferredCoopSize: Int
        ): List<Coop> = transaction {
            List((farmers.count() / preferredCoopSize) + 1) { index ->
                Coop.new {
                    this.contract = contract.id
                    this.name = Config.coopIncrementChar.plus(index).toString() +
                            Config.coopName +
                            contract.maxCoopSize
                }
            }
        }

        fun createRollCall(farmers: List<Farmer>, contract: EggInc.Contract): List<Coop> {
            val activeFarmers = farmers.filter { it.isActive }.sortedByDescending { it.earningsBonus }
            val inactiveFarmers = farmers.filter { !it.isActive }
            val preferredCoopSize: Int =
                if (contract.maxCoopSize <= 10) contract.maxCoopSize
                else (contract.maxCoopSize * FILL_PERCENTAGE).roundToInt()
            val coops = createCoops(farmers, contract, preferredCoopSize)

            transaction {
                // Fill each co-op with the next strongest player so that all co-ops have one
                coops.forEachIndexed { i, coop ->
                    coop.farmers = SizedCollection(coop.farmers.plus(activeFarmers[i]))
                }

                // With the remaining active farmers keep adding the next highest rated to the lowest rated co-op
                activeFarmers.drop(coops.size).forEach { activeFarmer ->
                    coops.filter { coop -> coop.farmers.count() <= preferredCoopSize }
                        .filter { coop -> coop.farmers.count() == coops.map { it.farmers.count() }.min() }
                        .minBy { coop -> coop.farmers.sumBy { it.earningsBonus } }!!
                        .let { coop -> coop.farmers = SizedCollection(coop.farmers.plus(activeFarmer)) }
                }

                // Finally spread inactive farmers over the coops
                inactiveFarmers.forEach { inactiveFarmer ->
                    coops.sortedBy { coop -> coop.farmers.count() }
                        .minBy { coop -> coop.farmers.count { farmer -> !farmer.isActive } }!!
                        .let { coop -> coop.farmers = SizedCollection(coop.farmers.plus(inactiveFarmer)) }
                }
            }
            return coops
        }
    }
}
