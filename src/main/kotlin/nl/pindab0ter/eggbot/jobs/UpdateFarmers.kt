package nl.pindab0ter.eggbot.jobs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.helpers.asyncMap
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.Job
import org.quartz.JobExecutionContext


class UpdateFarmers : Job {
    override fun execute(context: JobExecutionContext?): Unit = runBlocking(Dispatchers.IO) {
        val farmers = transaction { Farmer.all().toList() }
        if (farmers.isEmpty()) return@runBlocking logger.info { "No farmers to update…" }

        farmers.asyncMap { farmer ->
            AuxBrain.getFarmerBackup(farmer.inGameId)?.let {
                transaction { farmer.update(it) }
            }
        }.filterNotNull()

        logger.info { "Updated ${farmers.size} farmers and their known contracts." }
    }

    companion object {
        val logger = KotlinLogging.logger { }
    }
}
