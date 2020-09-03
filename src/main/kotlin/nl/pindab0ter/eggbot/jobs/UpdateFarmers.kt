package nl.pindab0ter.eggbot.jobs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.Job
import org.quartz.JobExecutionContext
import kotlin.streams.toList


class UpdateFarmers : Job {

    private val log = KotlinLogging.logger {}

    override fun execute(context: JobExecutionContext?) {
        val farmers = transaction { Farmer.all().toList() }
        if (farmers.isEmpty()) {
            log.info { "No farmers to update…" }
            return
        }

        runBlocking(Dispatchers.IO) {
            farmers.parallelStream().map { farmer ->
                farmer to AuxBrain.getFarmerBackup(farmer.inGameId)
            }.toList().let { farmers ->
                transaction {
                    farmers.forEach { (farmer, backup) -> backup?.let { farmer.update(it) } }
                }
                log.info { "Updated ${farmers.size} farmers and their known contracts." }
            }
        }
    }
}
