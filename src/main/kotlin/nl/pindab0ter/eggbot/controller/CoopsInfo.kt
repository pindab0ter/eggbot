package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.controller.categories.ContractsCategory
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.ProgressBar
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.Companion.initialEggsLaidComparator
import nl.pindab0ter.eggbot.view.coopsInfoResponse
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.time.ExperimentalTime

object CoopsInfo : EggBotCommand() {

    init {
        category = ContractsCategory
        name = "coops"
        help = "Shows info on all known co-ops for the specified contract."
        parameters = listOf(
            contractIdOption,
            forceReportedOnlySwitch,
            compactSwitch
        )
        sendTyping = false
        init()
    }

    @ExperimentalTime
    override fun execute(event: CommandEvent, parameters: JSAPResult) = runBlocking {
        val contractId = parameters.getString(CONTRACT_ID)
        val catchUp = parameters.getBoolean(FORCE_REPORTED_ONLY).not()
        val compact = parameters.getBoolean(COMPACT)

        val coops = transaction { Coop.find { Coops.contractId eq contractId }.toList().sortedBy { it.name } }

        val message = event.channel.sendMessage("Looking for co-ops…").complete()

        val contract = AuxBrain.getContract(contractId) ?: return@runBlocking event.replyAndLogWarning(
            "Could not find any co-ops for contract id `$contractId`. Is `contract id` correct and are there registered teams?"
        ).also { message.delete().queue() }

        val progressBar = ProgressBar(coops.size, message, coroutineContext = coroutineContext)

        // TODO: Let CoopContractStatus update ProgressBar since requesting Backups takes a lot of the time?
        val statuses = coops.asyncMap(coroutineContext) status@{ coop ->
            val status = CoopContractStatus(contract, coop.name, catchUp)
            progressBar.update()
            status
        }.let { statuses ->
            if (!compact) statuses.sortedDescending()
            else statuses.sortedWith(initialEggsLaidComparator).reversed()
        }

        message.delete().queue()

        if (statuses.isEmpty()) return@runBlocking event.replyAndLogWarning(
            "Could not find any co-ops for contract id `$contractId`.\nIs `contract id` correct and are there registered teams?"
        )

        coopsInfoResponse(contract, statuses, compact).let { messages ->
            messages.forEach { message -> event.reply(message) }
        }
    }
}
