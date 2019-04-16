package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import nl.pindab0ter.eggbot.*
import nl.pindab0ter.eggbot.commands.rollcall.PaddingDistribution
import nl.pindab0ter.eggbot.commands.rollcall.SequentialDistribution
import nl.pindab0ter.eggbot.commands.rollcall.SnakingDistribution
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.CoopFarmers
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.network.AuxBrain
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object RollCall : Command() {
    init {
        name = "rc"
        arguments = "<contract id> [algorithm]"
        aliases = arrayOf("rollcall", "roll-call")
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
            val coopsExist = Coops.select { Coops.contractId eq contractInfo.identifier }.any()

            if (coopsExist) {
                event.replyWarning("Co-ops are already generated for contract `${contractInfo.identifier}`")
                return@transaction
            }

            val farmers = transaction { Farmer.all().sortedByDescending { it.earningsBonus }.toList() }
            val coops: List<Coop> = algorithm.createRollCall(farmers, contractInfo)

            event.reply(StringBuilder("Co-ops generated for `${contractInfo.identifier}`:").appendln().apply {
                append("```")
                coops.forEach { coop ->
                    append(coop.name)
                    append(" (")
                    appendPaddingSpaces(coop.farmers.count(), coops.map { it.farmers.count() })
                    append(coop.farmers.count())
                    append(" members): ")
                    appendPaddingSpaces(coop.earningsBonus, coops.map { it.earningsBonus })
                    append(coop.earningsBonus.formatAsEB())
                    appendln()
                }
                append("```")
            }.toString())

            coops.forEach { coop ->
                event.reply(StringBuilder("Co-op `${coop.name}`:").appendln().apply {
                    append("```")
                    coop.farmers.forEach { farmer ->
                        append(farmer.inGameName)
                        append(": ")
                        appendPaddingSpaces(
                            farmer.inGameName,
                            coops.flatMap { coop -> coop.farmers.map { it.inGameName } })
                        appendPaddingSpaces(
                            farmer.earningsBonus.formatAsEB(),
                            coop.farmers.map { it.earningsBonus.formatAsEB() })
                        append(farmer.earningsBonus.formatAsEB())
                        appendln()
                    }
                    append("```")
                }.toString())
            }
        }
    }
}
