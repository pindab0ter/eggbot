package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import nl.pindab0ter.eggbot.arguments
import nl.pindab0ter.eggbot.commands.coops.PlaceholderDistribution
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.format
import nl.pindab0ter.eggbot.network.AuxBrain
import nl.pindab0ter.eggbot.sum
import nl.pindab0ter.eggbot.sumBy
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.contracts.ExperimentalContracts

object RollCall : Command() {
    init {
        name = "rc"
        arguments = "<contract id>"
        aliases = arrayOf("rollcall", "roll-call")
        help = "Create a co-op roll call for the given contract id"
        // TODO: Make guild only
        guildOnly = false
    }

    @ExperimentalContracts
    override fun execute(event: CommandEvent) {
        if (event.arguments.count() < 1) {
            event.replyWarning("Missing argument(s). See `${event.client.textualPrefix}${event.client.helpWord}` for more information")
            return
        }
        if (event.arguments.count() > 1) {
            event.replyWarning("Too many arguments. See `${event.client.textualPrefix}${event.client.helpWord}` for more information")
            return
        }

        val contractInfo = AuxBrain.getContracts().contractsList.find { it.identifier == event.arguments.first() }

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

            val coops = PlaceholderDistribution.createCoops(contractInfo)

            // TODO: Fix spacing
            //@formatter:off
            event.reply(StringBuilder("Co-ops generated for `${contractInfo.identifier}`:").appendln().apply {
                append("```")
                coops.forEach { coop ->
                    append("${coop.name}: ")
                    append(format(coop.farmers
                        .map { it.earningsBonus }.sum()
                    ))
                    appendln()
                }
                append("Available EB: ")
                append(format(coops
                    .flatMap { coop -> coop.farmers }
                    .sumBy { it.earningsBonus }
                ))
                append("```")
            }.toString())
            //@formatter:on
        }
    }
}
