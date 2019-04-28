package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.*
import nl.pindab0ter.eggbot.database.*
import nl.pindab0ter.eggbot.network.AuxBrain
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.roundToInt

object RollCall : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "roll-call"
        arguments = "<contract id>"
        aliases = arrayOf("rc", "rollcall")
        help = "Create a co-op roll call for the given contract id"
        // category = ContractsCategory
        guildOnly = false
    }

    // TODO: Add dry run option
    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(event: CommandEvent) {
        if (event.arguments.count() < 1) "Missing argument(s). See `${event.client.textualPrefix}${event.client.helpWord}` for more information".let {
            event.replyWarning(it)
            log.trace { it }
            return
        }
        if (event.arguments.count() > 1) "Too many arguments. See `${event.client.textualPrefix}${event.client.helpWord}` for more information".let {
            event.replyWarning(it)
            log.trace { it }
            return
        }

        // if (Config.devMode) transaction {
        transaction {
            Coops.deleteAll()
            CoopFarmers.deleteAll()
        }

        val contractInfo = AuxBrain.getContracts().contractsList.find { it.identifier == event.arguments.first() }

        if (contractInfo == null) "No active contract found with id `${event.arguments.first()}`".let {
            event.replyWarning(it)
            log.trace { it }
            return
        }

        if (contractInfo.coopAllowed != 1) "Co-op is not allowed for this contract".let {
            event.replyWarning(it)
            log.trace { it }
            return
        }

        transaction {
            val contract = Contract.getOrNew(contractInfo)
            if (!contract.coops.empty()) "Co-ops are already generated for contract `${contract.identifier}`".let {
                event.replyWarning(it)
                log.trace { it }
                return@transaction
            }

            log.info { "Generating schedule for ${contract.identifier}" }
            val farmers = transaction { Farmer.all().sortedByDescending { it.earningsBonus }.toList() }
            val coops: List<Coop> = PaddingDistribution.createRollCall(farmers, contract)

            event.reply(StringBuilder("Co-ops generated for `${contract.identifier}`:").appendln().apply {
                append("```")
                coops.forEach { coop ->
                    append(coop.name)
                    append(" (")
                    appendPaddingSpaces(coop.farmers.count(), coops.map { it.farmers.count() })
                    append(coop.farmers.count())
                    append("/${contract.maxCoopSize} members): ")
                    appendPaddingSpaces(coop.activeEarningsBonus, coops.map { it.activeEarningsBonus })
                    append(coop.activeEarningsBonus.formatForDisplay() + " %")
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
                        appendPaddingSpaces(
                            farmer.inGameName,
                            coops.flatMap { coop -> coop.farmers.map { it.inGameName } })
                        appendPaddingSpaces(
                            farmer.earningsBonus.formatForDisplay() + " %",
                            coop.farmers.map { it.earningsBonus.formatForDisplay() + " %" })
                        append(farmer.earningsBonus.formatForDisplay() + " %")
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

        private fun createCoops(farmers: List<Farmer>, contract: Contract): List<Coop> = transaction {
            List(((farmers.count() * 1.2) / contract.maxCoopSize).toInt() + 1) { index ->
                Coop.new {
                    this.contract = contract
                    this.name = Config.coopIncrementChar.plus(index).toString() +
                            Config.coopName +
                            contract.maxCoopSize
                }
            }
        }

        fun createRollCall(farmers: List<Farmer>, contract: Contract): List<Coop> {
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
                    coops.filter { coop -> coop.farmers.count() < preferredCoopSize }
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
