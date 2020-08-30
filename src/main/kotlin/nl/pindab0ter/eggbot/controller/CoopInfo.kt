package nl.pindab0ter.eggbot.controller

import com.auxbrain.ei.Backup
import com.auxbrain.ei.CoopStatusResponse
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
import nl.pindab0ter.eggbot.model.database.Contract
import nl.pindab0ter.eggbot.model.simulation.new.simulateCoopContract
import nl.pindab0ter.eggbot.view.coopFinishedResponse
import nl.pindab0ter.eggbot.view.coopInfoResponse
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.streams.toList
import kotlin.text.RegexOption.DOT_MATCHES_ALL

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

    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val contractId: String = parameters.getString(CONTRACT_ID)
        val coopId: String = parameters.getString(COOP_ID)
        val compact: Boolean = parameters.getBoolean(COMPACT, false)
        val forceReportedOnly: Boolean = parameters.getBoolean(FORCE_REPORTED_ONLY, false)
        val message: Message = event.channel.sendMessage("Fetching contract information…").complete()
        val coopStatus: CoopStatusResponse = AuxBrain.getCoopStatus(contractId, coopId)
            ?: "No co-op found for contract `${contractId}` with name `${coopId}`".let {
                message.delete().queue()
                event.replyWarning(it)
                log.debug { it }
                return
            }

        val contract: Contract = transaction {
            Contract.findById(coopStatus.contractId) ?: AuxBrain.getFarmerBackup(coopStatus.creatorId)?.let { backup ->
                val contract = backup.contracts?.contracts?.find { localContract ->
                    localContract.contract?.id == coopStatus.contractId
                } ?: backup.contracts?.archive?.last { localContract ->
                    localContract.contract?.id == coopStatus.contractId
                }
                if (contract == null) null
                else Contract.new(contract.contract!!.id) {
                    name = contract.contract.name
                    finalGoal = contract.contract.finalGoal
                }
            }
        } ?: "Could not find contract information".let {
            message.delete().queue()
            event.replyWarning(it)
            log.debug { it }
            return
        }

        if (coopStatus.contributors.isEmpty()) """
            `${coopStatus.coopId}` vs. __${contract.name}__:
                
            This co-op has no members.""".trimIndent().let {
            message.delete().queue()
            event.replyWarning(it)
            log.debug { it.replace("""\s+""".toRegex(DOT_MATCHES_ALL), " ") }
            return
        }

        // TODO: Incorporate grace period; if simulation says they'll make it if everyone checks in, don't show this
        if (coopStatus.secondsRemaining < 0.0 && coopStatus.totalAmount.toBigDecimal() < contract.finalGoal) """
            `${coopStatus.coopId}` vs. __${contract.name}__:
                
            This co-op has not reached their final goal.""".trimIndent().let {
            message.delete().queue()
            event.replyWarning(it)
            log.debug { it.replace("""\s+""".toRegex(DOT_MATCHES_ALL), " ") }
            return
        }

        message.editMessage("Fetching backups…").queue()
        message.channel.sendTyping().queue()

        val backups: List<Backup> = coopStatus.contributors.parallelStream().map { farmer ->
            AuxBrain.getFarmerBackup(farmer.userId)
        }.toList().filterNotNull()

        message.editMessage("Running simulation…").queue()
        message.channel.sendTyping().queue()

        // TODO: Check if backups.count == contributors.count, else add backup not found to message
        // TODO: Check if backups.findFarm.count == contributors.count, else farm not found to message

        if (coopStatus.eggsLaid >= contract.finalGoal) {
            val state = simulateCoopContract(backups, contractId, coopStatus, catchUp = false)

            if (state == null || state.farmers.count() != coopStatus.contributors.count()) """
            `${coopStatus.coopId}` vs. __${contract.name}__:

            This co-op has successfully finished their contract! ${Config.emojiSuccess}""".trimIndent().let {
                message.delete().queue()
                event.reply(it)
                log.debug { it.replace("""\s+""".toRegex(DOT_MATCHES_ALL), " ") }
                return
            }

            message.delete().queue()

            coopFinishedResponse(state, compact).let { messages ->
                if (event.channel == botCommandsChannel) {
                    messages.forEach { message -> event.reply(message) }
                } else {
                    event.replyInDms(messages)
                    if (event.isFromType(ChannelType.TEXT)) event.reactSuccess()
                }
                return
            }
        }
        val startSimulation = System.currentTimeMillis()
        val state = simulateCoopContract(backups, contractId, coopStatus, catchUp = !forceReportedOnly).let { state ->
            state?.copy(
                farmers = state.farmers.sortedByDescending { farmer -> farmer.initialState.eggsLaid }
            )
        }
        log.debug { "Simulation took ${System.currentTimeMillis() - startSimulation}ms" }

        message.delete().queue()

        if (state == null) """
            `${coopStatus.coopId}` vs. __${contract.name}__:
                
            Everyone has left this coop.""".trimIndent().let {
            event.replyWarning(it)
            log.debug { it.replace("""\s+""".toRegex(DOT_MATCHES_ALL), " ") }
            return
        }

        // TODO: Create sorting flag
        // TODO: Sort by other things, e.g. chickens

        (when {
            !forceReportedOnly && state.finished ->
                coopFinishedResponse(state, compact, ifCheckedIn = true)
            else ->
                coopInfoResponse(state, compact)
        }).let { messages ->
            if (event.channel == botCommandsChannel) {
                messages.forEach { message -> event.reply(message) }
            } else {
                event.replyInDms(messages)
                if (event.isFromType(ChannelType.TEXT)) event.reactSuccess()
            }
        }
    }
}
