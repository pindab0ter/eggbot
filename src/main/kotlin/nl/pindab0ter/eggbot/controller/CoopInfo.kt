package nl.pindab0ter.eggbot.controller

import com.auxbrain.ei.Backup
import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Message
import nl.pindab0ter.eggbot.EggBot.botCommandsChannel
import nl.pindab0ter.eggbot.controller.categories.ContractsCategory
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.Config
import nl.pindab0ter.eggbot.model.simulation.new.simulateCoopContract
import nl.pindab0ter.eggbot.view.coopInfoResponseNew

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
            compactSwitch
        )
        sendTyping = false
        init()
    }

    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val contractId: String = parameters.getString(CONTRACT_ID)
        val coopId: String = parameters.getString(COOP_ID)
        val compact: Boolean = parameters.getBoolean(COMPACT, false)
        val message: Message = event.channel.sendMessage("Fetching contract information…").complete()
        val coopStatus = AuxBrain.getCoopStatus(contractId, coopId)
        val contractName = AuxBrain.getPeriodicals()?.contracts?.contracts?.find { it.id == contractId }?.name
            ?: "Unknown"

        if (coopStatus == null) "No co-op found for contract `${contractId}` with name `${coopId}`".let {
            message.delete().queue()
            event.replyWarning(it)
            log.debug { it }
            return
        }
        if (coopStatus.contributors.isEmpty()) """ `${coopStatus.coopId}` vs. __${contractName}__:
                
                This co-op has no members.""".let {
            message.delete().queue()
            event.replyWarning(it)
            log.debug { it }
            return
        }

        message.editMessage("Fetching backups…").queue()
        message.channel.sendTyping().queue()

        val backups: List<Backup> = runBlocking(Dispatchers.IO) {
            coopStatus.contributors.asyncMap { AuxBrain.getFarmerBackup(it.userId) }
        }.filterNotNull()

        val localContract = backups.findContract(contractId, coopStatus.creatorId)
            ?: "Could not find contract information".let {
                message.delete().queue()
                event.replyWarning(it)
                log.debug { it }
                return
            }

        // Has the co-op failed?
        // TODO: Incorporate grace period
        if (coopStatus.secondsRemaining < 0.0 && coopStatus.totalAmount.toBigDecimal() < localContract.finalGoal)
            """ `${coopStatus.coopId}` vs. __${contractName}__:
                
                This co-op has not reached their final goal.""".trimIndent().let {
                message.delete().queue()
                event.replyWarning(it)
                log.debug { it }
                return
            }

        // TODO: Check whether co-op is finished before finding the local backup, which can fail
        // TODO: Save final goal in DB?
        //
        // Co-op finished?
        //
        // The amount of eggs laid according to the co-op status is higher than the final goal
        // or
        // There is no active farm with this contract
        // and the contract archive contains this contract
        // and that contract has reached its final goal
        // for any of the contributors
        if (coopStatus.eggsLaid >= localContract.finalGoal || backups.any { contributor ->
                contributor.farms.none { farm ->
                    farm.contractId == coopStatus.contractId
                } && contributor.contracts!!.archive.find { contract ->
                    contract.contract!!.id == coopStatus.contractId
                }?.finished == true
            }
        ) """ `${coopStatus.coopId}` vs. __${contractName}__:
                
                This co-op has successfully finished their contract! ${Config.emojiSuccess}""".trimIndent().let {
            message.delete().queue()
            event.reply(it)
            log.debug { it }
            return
        }


        message.editMessage("Running simulation…").queue()
        message.channel.sendTyping().queue()

        val coopContractState = simulateCoopContract(backups, contractId, coopStatus)

        message.delete().queue()

        coopInfoResponseNew(coopContractState, compact).let { messages ->
            if (event.channel == botCommandsChannel) {
                messages.forEach { message -> event.reply(message) }
            } else {
                event.replyInDms(messages)
                if (event.isFromType(ChannelType.TEXT)) event.reactSuccess()
            }
        }
    }
}
