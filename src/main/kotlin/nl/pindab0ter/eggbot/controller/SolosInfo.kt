package nl.pindab0ter.eggbot.controller

import com.auxbrain.ei.Backup
import com.auxbrain.ei.Contract
import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.controller.categories.ContractsCategory
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.ProgressBar
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.model.simulation.SoloContractState
import nl.pindab0ter.eggbot.model.simulation.simulate
import nl.pindab0ter.eggbot.view.solosInfoResponse
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.time.ExperimentalTime

object SolosInfo : EggBotCommand() {

    init {
        category = ContractsCategory
        name = "solos"
        help = "Shows info on all farmers working on the specified contract."
        parameters = listOf(
            contractIdOption,
            compactSwitch
        )
        sendTyping = false
        init()
    }

    @ExperimentalTime
    override fun execute(event: CommandEvent, parameters: JSAPResult) = runBlocking {
        val contractId = parameters.getString(CONTRACT_ID).toLowerCase()
        val compact = parameters.getBoolean(COMPACT, false)

        val message = event.channel.sendMessage("Looking up contract information ").complete()
        message.channel.sendTyping().queue()

        val databaseFarmers = transaction { Farmer.all().toList() }

        val progressBar = ProgressBar(
            goal = databaseFarmers.count(),
            message = message,
            statusText = "Looking up which farmers are attempting `${contractId}`…",
            unit = "registered farmers",
            coroutineContext
        )

        val farmers: List<Backup> = databaseFarmers.asyncMap() { databaseFarmer ->
            AuxBrain.getFarmerBackup(databaseFarmer.inGameId).also {
                progressBar.increment()
            }
        }.filterNotNull().filter { farmer ->
            farmer.farms.any { farm ->
                farm.contractId == contractId
            }
        }

        if (farmers.isEmpty()) {
            progressBar.stop()
            return@runBlocking event.replyAndLogWarning(
                "Could not find any registered farmers currently attempting contract id `$contractId`."
            )
        }

        progressBar.reset(
            goal = farmers.count(),
            statusText = "Simulating…",
            unit = "farms",
        )

        val states = farmers.asyncMap { farmer ->
            farmer.contracts?.contracts?.find { localContract ->
                localContract.contract?.id == contractId
            }?.let { localContract ->
                SoloContractState(farmer, localContract)?.let {
                    simulate(it).also {
                        progressBar.increment()
                    }
                }
            }
        }.filterNotNull()

        val contract: Contract = farmers.asSequence().mapNotNull { farmer ->
            farmer.contracts?.contracts
        }.flatten().toSet().map { it.contract }.find { contract ->
            contract?.id == contractId
        } ?: return@runBlocking event.replyAndLogWarning(
            "Nobody is soloing a contract with ID `$contractId`. Are you sure the contract ID is correct?"
        )

        progressBar.stop()

        if (states.isEmpty()) return@runBlocking event.replyAndLogWarning(
            "Could not find any registered farmers currently attempting contract id `$contractId`."
        )

        solosInfoResponse(contract, states, compact).let { messages ->
            messages.forEach { message -> event.reply(message) }
        }
    }
}
