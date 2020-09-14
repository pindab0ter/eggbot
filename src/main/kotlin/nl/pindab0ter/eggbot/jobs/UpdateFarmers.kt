package nl.pindab0ter.eggbot.jobs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.helpers.asyncMap
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.database.Farmer
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.Job
import org.quartz.JobExecutionContext


class UpdateFarmers : Job, Logging {

    override fun execute(context: JobExecutionContext?): Unit = runBlocking(Dispatchers.IO) {
        val farmers = transaction { Farmer.all().toList() }
        if (farmers.isEmpty()) {
            logger.info { "No farmers to update…" }
            return@runBlocking
        }

        farmers.asyncMap { farmer ->
            AuxBrain.getFarmerBackup(farmer.inGameId)?.let {
                transaction { farmer.update(it) }
            }
        }.filterNotNull()

        logger.info { "Updated ${farmers.size} farmers and their known contracts." }
    }
}
