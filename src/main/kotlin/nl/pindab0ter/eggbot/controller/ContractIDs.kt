package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import nl.pindab0ter.eggbot.controller.categories.ContractsCategory
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.view.contractIDsResponse

object ContractIDs : EggBotCommand() {

    init {
        category = ContractsCategory
        name = "contract-ids"
        aliases = arrayOf("ids")
        help = "Shows the IDs of the currently active contracts"
        sendTyping = false
        init()
    }

    override fun execute(
        event: CommandEvent,
        parameters: JSAPResult,
    ) = AuxBrain.getContracts { contracts ->
        val (soloContracts, coopContracts) = contracts
            ?.sortedBy { it.expirationTime }
            ?.groupBy { it.coopAllowed }
            ?.let { it[false].orEmpty() to it[true].orEmpty() }
            ?: return@getContracts event.replyWarning("Could not get the active contracts.")

        if (soloContracts.isNotEmpty() && coopContracts.isNotEmpty())
            event.reply(contractIDsResponse(soloContracts, coopContracts))
        else event.replyWarning("There are currently no active contracts")
    }
}
