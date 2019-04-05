package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import nl.pindab0ter.eggbot.arguments
import nl.pindab0ter.eggbot.commands.coops.PlaceholderDistribution
import nl.pindab0ter.eggbot.format
import nl.pindab0ter.eggbot.network.AuxBrain
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

        // TODO: Check if coops already exist for this contract

        val coops = PlaceholderDistribution.createCoops(contractInfo)

        // TODO: Replace with StringBuilder
        val coopsString = coops.joinToString("\n") { coop ->
            "${coop.name}         ${format(coop.farmers.map { it.earningsBonus }.sum())}"
        }

        val totalEbString = format(coops.map { coop ->
            coop.farmers.map { farmer ->
                farmer.earningsBonus
            }.sum()
        }.sum())

        event.reply(
            "Coops: ```$coopsString\nTotal available EB: $totalEbString```"
        )
    }
}
