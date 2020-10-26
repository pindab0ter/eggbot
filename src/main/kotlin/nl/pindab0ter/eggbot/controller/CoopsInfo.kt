package nl.pindab0ter.eggbot.controller

import com.auxbrain.ei.CoopStatusResponse
import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.controller.categories.ContractsCategory
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.Config
import nl.pindab0ter.eggbot.model.ProgressBar
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.Companion.currentEggsComparator
import nl.pindab0ter.eggbot.view.coopsInfoResponse
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import kotlin.time.ExperimentalTime

object CoopsInfo : EggBotCommand() {

    init {
        category = ContractsCategory
        name = "coops"
        help = "Shows info on all known co-ops for the specified contract."
        parameters = listOf(
            contractIdOption,
            compactSwitch
        )
        sendTyping = false
        init()
    }

    @ExperimentalTime
    override fun execute(event: CommandEvent, parameters: JSAPResult) = runBlocking {
        val contractId = parameters.getString(CONTRACT_ID)
        val compact = parameters.getBoolean(COMPACT, false)

        val contract = AuxBrain.getContract(contractId) ?: return@runBlocking event.replyAndLogWarning(
            "Could not find a contract with ID `$contractId`. Use `${Config.prefix}${ContractIDs.name}` to get a list of the current contracts."
        )

        val coops = transaction { Coop.find { Coops.contractId eq contractId }.toList().sortedBy { it.name } }

        if (coops.isEmpty()) return@runBlocking event.replyAndLogWarning(
            "Could not find any co-ops for contract id `$contractId`. To register new contracts use `${Config.prefix}${CoopAdd.name}` or create a new roll call using `${Config.prefix}${RollCall.name}`"
        )

        val message = event.channel.sendMessage("Fetching co-op statuses…").complete()
        message.channel.sendTyping().queue()

        val coopStatuses: List<CoopStatusResponse?> = coops.asyncMap(coroutineContext) status@{ coop ->
            AuxBrain.getCoopStatus(contract.id, coop.name)
        }

        val progressBar = ProgressBar(
            goal = coopStatuses.sumBy { coopStatus ->
                if ((coopStatus?.eggsLaid ?: BigDecimal.ZERO) < contract.goals.last().targetAmount.toBigDecimal()) {
                    coopStatus?.contributors?.count() ?: 0
                } else 0
            },
            message = message,
            statusText = "Fetching backups and running simulations…",
            unit = "simulations",
            coroutineContext = coroutineContext
        )

        val statuses = coopStatuses
            .zip(coops.map { coop -> coop.name })
            .asyncMap(coroutineContext) { (coopStatus, coopId) ->
                CoopContractStatus(contract, coopStatus, coopId, progressCallback = progressBar::update)
            }.let { statuses ->
                if (!compact) statuses.sortedWith(currentEggsComparator)
                else statuses.sortedWith(currentEggsComparator)
            }

        progressBar.stopAndDeleteMessage()

        if (statuses.isEmpty()) return@runBlocking event.replyAndLogWarning(
            "Could not find any co-ops for contract id `$contractId`.\nIs `contract id` correct and are there registered teams?"
        )

        coopsInfoResponse(contract, statuses, compact).let { messages ->
            messages.forEach { message -> event.reply(message) }
        }
    }
}
