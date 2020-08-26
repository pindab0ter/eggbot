package nl.pindab0ter.eggbot.jobs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.helpers.asyncMap
import nl.pindab0ter.eggbot.helpers.finalGoal
import nl.pindab0ter.eggbot.helpers.findOrCreateById
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.database.Contract
import nl.pindab0ter.eggbot.model.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.Job
import org.quartz.JobExecutionContext


class UpdateFarmersAndContractsJob : Job {

    private val log = KotlinLogging.logger {}

    override fun execute(context: JobExecutionContext?) {
        val farmers = transaction { Farmer.all().toList() }
        if (farmers.isEmpty()) {
            log.info { "No farmers to update…" }
            return
        }

        runBlocking(Dispatchers.IO) {
            farmers
                .asyncMap { farmer ->
                    farmer to AuxBrain.getFarmerBackup(farmer.inGameId)
                }
                .let { farmers ->
                    transaction {
                        farmers.flatMap { (_, backup) ->
                            backup?.contracts?.contracts.orEmpty()
                        }.distinct().forEach { localContract ->
                            Contract.findOrCreateById(localContract.contract!!.id) {
                                name = localContract.contract.name
                                finalGoal= localContract.contract.finalGoal
                            }
                        }

                        farmers.forEach { (farmer, backup) -> backup?.let { farmer.update(it) } }
                    }
                    log.info { "Updated ${farmers.size} farmers and their known contracts." }
                }
        }
    }
}
