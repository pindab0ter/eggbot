package nl.pindab0ter.eggbot.controller

import com.auxbrain.ei.Contract
import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import mu.KotlinLogging
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
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.InProgress.FinishedIfCheckedIn
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.NotFound
import nl.pindab0ter.eggbot.view.coopFinishedIfCheckedInResponse
import nl.pindab0ter.eggbot.view.coopInfoResponse
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@Suppress("FoldInitializerAndIfToElvis")
object CoopInfo : EggBotCommand() {

    private val log = KotlinLogging.logger { }

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
            forceReportedOnlySwitch
        )
        sendTyping = false
        init()
    }

    @ExperimentalTime
    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val contractId: String = parameters.getString(CONTRACT_ID)
        val coopId: String = parameters.getString(COOP_ID)
        val compact: Boolean = parameters.getBoolean(COMPACT, false)
        val catchUp: Boolean = parameters.getBoolean(FORCE_REPORTED_ONLY, false).not()

        val message: Message = event.channel.sendMessage("Fetching required information…").complete()

        val contract: Contract = AuxBrain.getContract(contractId)
            ?: "Could not find contract information".let {
                message.delete().queue()
                event.replyWarning(it)
                log.debug { it }
                return
            }

        message.editMessage("Running simulation…").queue()
        message.channel.sendTyping().queue()


        val (status, duration) = measureTimedValue {
            CoopContractStatus(contract, coopId, catchUp)
        }

        log.debug { "Simulation took $duration" }

        when (status) {
            is NotFound -> "No co-op found for contract `${contractId}` with name `${coopId}`".let {
                message.delete().queue()
                event.replyWarning(it)
                log.debug { it }
                return
            }
            is Abandoned -> """
                `${status.coopStatus.coopId}` vs. __${contract.name}__:
                    
                This co-op has no members.""".trimIndent().let {
                message.delete().queue()
                event.replyWarning(it)
                log.debug { it.replace("""\s+""".toRegex(DOT_MATCHES_ALL), " ") }
                return
            }
            is Failed -> """
                `${status.coopStatus.coopId}` vs. __${contract.name}__:
                    
                This co-op has not reached their final goal.""".trimIndent().let {
                message.delete().queue()
                event.reply(it)
                log.debug { it.replace("""\s+""".toRegex(DOT_MATCHES_ALL), " ") }
                return
            }
            is Finished -> """
                `${status.coopStatus.coopId}` vs. __${contract.name}__:
    
                This co-op has successfully finished their contract! ${Config.emojiSuccess}""".trimIndent().let {
                message.delete().queue()
                event.reply(it)
                log.debug { it.replace("""\s+""".toRegex(DOT_MATCHES_ALL), " ") }
                return
            }
            is InProgress -> {
                val sortedState = status.state.copy(
                    farmers = status.state.farmers.sortedByDescending { farmer -> farmer.finalState.eggsLaid }
                )

                when (status) {
                    is FinishedIfCheckedIn -> coopFinishedIfCheckedInResponse(sortedState, compact)
                    else -> coopInfoResponse(sortedState, compact)
                }.let { messages ->
                    message.delete().queue()
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
