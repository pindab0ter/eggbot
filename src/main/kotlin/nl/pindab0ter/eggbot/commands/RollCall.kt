package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import nl.pindab0ter.eggbot.*
import nl.pindab0ter.eggbot.commands.rollcall.PaddingDistribution
import nl.pindab0ter.eggbot.commands.rollcall.SequentialDistribution
import nl.pindab0ter.eggbot.commands.rollcall.SnakingDistribution
import nl.pindab0ter.eggbot.database.*
import nl.pindab0ter.eggbot.network.AuxBrain
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction

object RollCall : Command() {
    init {
        name = "roll-call"
        arguments = "<contract id>"
        aliases = arrayOf("rc", "rollcall")
        help = "Create a co-op roll call for the given contract id"
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        if (event.arguments.count() < 1) {
            event.replyWarning("Missing argument(s). See `${event.client.textualPrefix}${event.client.helpWord}` for more information")
            return
        }
        if (event.arguments.count() > 2) {
            event.replyWarning("Too many arguments. See `${event.client.textualPrefix}${event.client.helpWord}` for more information")
            return
        }

        if (Config.devMode) transaction {
            Coops.deleteAll()
            CoopFarmers.deleteAll()
        }

        val contractInfo = AuxBrain.getContracts().contractsList.find { it.identifier == event.arguments.first() }
        val algorithm = when (event.arguments.getOrNull(1)) {
            "sequential" -> SequentialDistribution
            "snaking" -> SnakingDistribution
            "padding" -> PaddingDistribution
            else -> PaddingDistribution
        }

        if (contractInfo == null) {
            event.replyWarning("No active contract found with id `${event.arguments.first()}`")
            return
        }

        if (contractInfo.coopAllowed != 1) {
            event.replyWarning("Co-op is not allowed for this contract")
            return
        }

        transaction {
            val contract = Contract.getOrNew(contractInfo)
            if (!contract.coops.empty()) {
                event.replyWarning("Co-ops are already generated for contract `${contract.identifier}`")
                return@transaction
            }

            val farmers = transaction { Farmer.all().sortedByDescending { it.earningsBonus }.toList() }
            val coops: List<Coop> = algorithm.createRollCall(farmers, contract)

            event.reply(StringBuilder("Co-ops generated for `${contract.identifier}`:").appendln().apply {
                append("```")
                coops.forEach { coop ->
                    append(coop.name)
                    append(" (")
                    appendPaddingSpaces(coop.farmers.count(), coops.map { it.farmers.count() })
                    append(coop.farmers.count())
                    append("/${contract.maxCoopSize} members): ")
                    appendPaddingSpaces(coop.activeEarningsBonus, coops.map { it.activeEarningsBonus })
                    append(coop.activeEarningsBonus.formatForDisplay())
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
                            farmer.earningsBonus.formatForDisplay(),
                            coop.farmers.map { it.earningsBonus.formatForDisplay() })
                        append(farmer.earningsBonus.formatForDisplay())
                        if (!farmer.isActive) append(" (Inactive)")
                        appendln()
                    }
                    append("```")
                }.toString()
            }.splitMessage(separator =  '\u000C')
                .forEach { message ->
                    event.reply(message)
                }
        }
    }
}
