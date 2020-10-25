package nl.pindab0ter.eggbot.controller

import com.auxbrain.ei.Contract
import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Message
import nl.pindab0ter.eggbot.EggBot.botCommandsChannel
import nl.pindab0ter.eggbot.controller.categories.ContractsCategory
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.Config
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.InActive.*
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.InProgress
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.InProgress.FinishedIfBanked
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.NotFound
import nl.pindab0ter.eggbot.view.coopFinishedIfBankedResponse
import nl.pindab0ter.eggbot.view.coopInfoResponse
import kotlin.time.ExperimentalTime

@Suppress("FoldInitializerAndIfToElvis")
object CoopInfo : EggBotCommand() {

    init {
        name = "coop"
        help = "Shows info on a specific co-op, displaying the current status, player contribution and runs a " +
                "simulation to estimate whether/when the goals will be reached and if people will reach their " +
                "habitat or transport bottlenecks."
        category = ContractsCategory
        parameters = listOf(
            contractIdOption,
            coopIdOption,
            compactSwitch,
        )
        sendTyping = false
        init()
    }

    @ExperimentalTime
    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val contractId: String = parameters.getString(CONTRACT_ID)
        val coopId: String = parameters.getString(COOP_ID)
        val compact: Boolean = parameters.getBoolean(COMPACT, false)
        val message: Message = event.channel.sendMessage("Fetching required information…").complete()
        message.channel.sendTyping().queue()

        val contract: Contract = AuxBrain.getContract(contractId) ?: return event.replyAndLogWarning(
            "Could not find contract information"
        ).also { message.delete().queue() }

        message.editMessage("Running simulation…").queue()
        message.channel.sendTyping().queue()

        val coopStatus = AuxBrain.getCoopStatus(contract.id, coopId)
        val status = CoopContractStatus(contract, coopStatus, coopId)

        message.delete().queue()

        when (status) {
            is NotFound -> event.replyAndLogWarning("No co-op found for contract `${contractId}` with name `${coopId}`")
            is Abandoned -> event.replyAndLog("""
                `${status.coopStatus.coopId}` vs. __${contract.name}__:
                    
                This co-op has no members.""".trimIndent())
            is Failed -> event.replyAndLog("""
                `${status.coopStatus.coopId}` vs. __${contract.name}__:
                    
                This co-op has not reached their final goal.""".trimIndent())
            is Finished -> event.replyAndLog("""
                `${status.coopStatus.coopId}` vs. __${contract.name}__:
    
                This co-op has successfully finished their contract! ${Config.emojiSuccess}""".trimIndent())
            is InProgress -> {
                val sortedState = status.state.copy(
                    farmers = status.state.farmers.sortedByDescending { farmer -> farmer.currentEggsLaid }
                )

                when (status) {
                    is FinishedIfBanked -> coopFinishedIfBankedResponse(sortedState, compact)
                    else -> coopInfoResponse(sortedState, compact)
                }.let { messages ->
                    when (event.channel) {
                        botCommandsChannel -> messages.forEach { message -> event.reply(message) }
                        else -> {
                            event.replyInDms(messages)
                            if (event.isFromType(ChannelType.TEXT)) event.reactSuccess()
                        }
                    }
                }
            }
        }
    }
}
