package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import mu.KotlinLogging
import nl.pindab0ter.eggbot.controller.categories.ContractsCategory
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.ProgressBar
import nl.pindab0ter.eggbot.model.ProgressBar.WhenDone
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus
import nl.pindab0ter.eggbot.view.coopsInfoResponse
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

object CoopsInfo : EggBotCommand() {

    private val log = KotlinLogging.logger { }

    init {
        category = ContractsCategory
        name = "coops"
        help = "Shows info on all known co-ops for the specified contract."
        parameters = listOf(
            contractIdOption,
            forceReportedOnlySwitch,
        )
        sendTyping = false
        init()
    }

    @ExperimentalTime
    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val contractId = parameters.getString(CONTRACT_ID)
        val catchUp = parameters.getBoolean(FORCE_REPORTED_ONLY).not()

        val coops = transaction { Coop.find { Coops.contractId eq contractId }.toList().sortedBy { it.name } }

        val message = event.channel.sendMessage("Looking for co-ops…").complete()
        val progressBar = ProgressBar(coops.size, message, WhenDone.STOP_IMMEDIATELY)

        val contract = AuxBrain.getContract(contractId)
            ?: "Could not find any co-ops for contract id `$contractId`.\nIs `contract id` correct and are there registered teams?".let {
                message.delete().queue()
                event.replyWarning(it)
                log.debug { it }
                return
            }

        val (statuses, duration) = measureTimedValue {
            coops.parallelMap status@{ coop ->
                val status = CoopContractStatus(contract, coop.name, catchUp)
                // TODO: Fix progress bar not updating
                progressBar.update()
                status
            }
        }

        log.debug { "Simulation took ${duration}ms" }

        if (statuses.isEmpty()) "Could not find any co-ops for contract id `$contractId`.\nIs `contract id` correct and are there registered teams?".let {
            message.delete().queue()
            event.replyWarning(it)
            log.debug { it }
            return
        }

        message.delete().queue()
        coopsInfoResponse(contract, statuses).let { messages ->
            messages.forEach { message -> event.reply(message) }
        }
    }
}
