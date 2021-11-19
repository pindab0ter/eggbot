package nl.pindab0ter.eggbot.jobs

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.helpers.asyncMap
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.Job
import org.quartz.JobExecutionContext
import kotlin.system.measureTimeMillis


class UpdateFarmers : Job {
    override fun execute(context: JobExecutionContext?): Unit = runBlocking {
        val farmers = transaction { Farmer.all().toList() }

        val timeTakenMillis = measureTimeMillis {
            farmers.asyncMap { farmer ->
                AuxBrain.getFarmerBackup(farmer.inGameId)?.let {
                    transaction { farmer.update(it) }
                }
            }
        }

        logger.info { "Updated ${farmers.size} farmers and their known contracts in ${timeTakenMillis}ms." }
    }

    companion object {
        val logger = KotlinLogging.logger { }
    }
}
