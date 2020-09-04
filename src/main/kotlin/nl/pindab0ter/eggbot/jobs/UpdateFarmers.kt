package nl.pindab0ter.eggbot.jobs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.helpers.parallelMap
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.Job
import org.quartz.JobExecutionContext


class UpdateFarmers : Job {

    private val log = KotlinLogging.logger {}

    override fun execute(context: JobExecutionContext?) {
        val farmers = transaction { Farmer.all().toList() }
        if (farmers.isEmpty()) {
            log.info { "No farmers to update…" }
            return
        }

        runBlocking(Dispatchers.IO) {
            farmers.parallelMap { farmer ->
                farmer to AuxBrain.getFarmerBackup(farmer.inGameId)
            }.let { farmers ->
                transaction {
                    farmers.forEach { (farmer, backup) -> backup?.let { farmer.update(it) } }
                }
                log.info { "Updated ${farmers.size} farmers and their known contracts." }
            }
        }
    }
}
