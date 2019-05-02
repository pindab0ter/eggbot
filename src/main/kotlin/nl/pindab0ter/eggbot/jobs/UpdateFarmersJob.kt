package nl.pindab0ter.eggbot.jobs

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.network.AuxBrain
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.Job
import org.quartz.JobExecutionContext


class UpdateFarmersJob : Job {

    private val log = KotlinLogging.logger {}

    override fun execute(context: JobExecutionContext?) {
        val farmers = transaction { Farmer.all().toList() }
        if (farmers.isEmpty()) {
            log.info { "No farmers to update…" }
            return
        }

        runBlocking {
            farmers
                .map { farmer -> farmer to GlobalScope.async { AuxBrain.getFarmerBackup(farmer.inGameId) } }
                .map { (farmer, backupJob) -> farmer to backupJob.await().get() }
                .let { farmers ->
                    transaction {
                        farmers.forEach { (farmer, backup) -> farmer.update(backup) }
                    }
                    log.info { "Updated ${farmers.size} farmers." }
                }
        }
    }
}
