package nl.pindab0ter.eggbot.commands

import com.auxbrain.ei.EggInc
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.*
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.network.AuxBrain
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.roundToInt

object RollCall : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "roll-call"
        arguments = "<contract id>"
        aliases = arrayOf("rc", "rollcall")
        help = "Create a co-op roll call for the given contract id"
        hidden = true
        // category = ContractsCategory
        guildOnly = false
    }

    // TODO: Add dry run option
    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        // If the admin role is defined check whether the author has at least that role or is the guild owner
        if (Config.rollCallRole != null && event.author.mutualGuilds.none { guild ->
                guild.getMember(event.author).let { author ->
                    author.isOwner || author.roles.any { memberRole ->
                        guild.getRolesByName(Config.rollCallRole, true)
                            .any { adminRole -> memberRole.position >= adminRole.position }
                    }
                }
            }) "You must have at least a role called `${Config.rollCallRole}` to use that!".let {
            event.replyError(it)
            log.debug { it }
            return
        }

        when {
            event.arguments.isEmpty() -> missingArguments.let {
                event.replyWarning(it)
                log.debug { it }
                return
            }
            event.arguments.size > 1 -> tooManyArguments.let {
                event.replyWarning(it)
                log.debug { it }
                return
            }
        }

        // if (Config.devMode) transaction {
        transaction {
            Coops.deleteAll()
            CoopFarmers.deleteAll()
        }

        val contractInfo: EggInc.Contract? = AuxBrain.getContracts().contractsList.find {
            it.identifier == event.arguments.first()
        }

        if (contractInfo == null) "No active contract found with id `${event.arguments.first()}`".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        if (contractInfo.coopAllowed != 1) "Co-op is not allowed for this contract".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        transaction {
            if (!Coops.select { Coops.contract eq contractInfo.identifier }.empty())
                "Co-ops are already generated for contract `${contractInfo.identifier}`. Add `force` to override.".let {
                    event.replyWarning(it)
                    log.debug { it }
                    return@transaction
                }

            val farmers = transaction { Farmer.all().sortedByDescending { it.earningsBonus }.toList() }
            val coops: List<Coop> = PaddingDistribution.createRollCall(farmers, contractInfo)
            val longestFarmerName = farmers.maxBy { it.inGameName.length }!!.inGameName
            val longestEarningsBonus = farmers
                .maxBy { it.earningsBonus.formatIllions(true).length }!!.earningsBonus.formatIllions(true)


            event.reply(StringBuilder("Co-ops generated for `${contractInfo.identifier}`:").appendln().apply {
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

        private fun createCoops(farmers: List<Farmer>, contract: EggInc.Contract): List<Coop> = transaction {
            List(((farmers.count() * 1.2) / contract.maxCoopSize).toInt() + 1) { index ->
                Coop.new {
                    this.contract = contract.identifier
                    this.name = Config.coopIncrementChar.plus(index).toString() +
                            Config.coopName +
                            contract.maxCoopSize
                }
            }
        }

        fun createRollCall(farmers: List<Farmer>, contract: EggInc.Contract): List<Coop> {
            val coops = createCoops(farmers, contract)
            val activeFarmers = farmers.filter { it.isActive }.sortedByDescending { it.earningsBonus }
            val inactiveFarmers = farmers.filter { !it.isActive }
            val preferredCoopSize = {
                val inactiveToActiveFarmerRatio = inactiveFarmers.size.toFloat() / farmers.size.toFloat()
                val activeFarmerFillRatio = FILL_PERCENTAGE - inactiveToActiveFarmerRatio * FILL_PERCENTAGE
                (contract.maxCoopSize * activeFarmerFillRatio).roundToInt()
            }()

            transaction {
                // Fill each co-op with the next strongest player so that all co-ops have one
                coops.forEachIndexed { i, coop ->
                    coop.farmers = SizedCollection(coop.farmers.plus(activeFarmers[i]))
                }

                // With the remaining active farmers keep adding the next highest rated to the lowest rated co-op
                activeFarmers.drop(coops.size).forEach { activeFarmer ->
                    coops.filter { coop -> coop.farmers.count() <= preferredCoopSize }
                        .filter { coop -> coop.farmers.count() == coops.map { it.farmers.count() }.min() }
                        .sortedBy { coop -> coop.farmers.sumBy { it.earningsBonus } }
                        .first()
                        .let { coop -> coop.farmers = SizedCollection(coop.farmers.plus(activeFarmer)) }
                }

                // Finally spread inactive farmers over the coops
                inactiveFarmers.forEach { inactiveFarmer ->
                    coops.sortedBy { coop -> coop.farmers.count() }
                        .sortedBy { coop -> coop.farmers.count { farmer -> !farmer.isActive } }
                        .first()
                        .let { coop -> coop.farmers = SizedCollection(coop.farmers.plus(inactiveFarmer)) }
                }
            }
            return coops
        }
    }
}
