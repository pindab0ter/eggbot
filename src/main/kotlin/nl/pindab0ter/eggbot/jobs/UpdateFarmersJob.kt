package nl.pindab0ter.eggbot.jobs

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.Job
import org.quartz.JobExecutionContext
import kotlin.system.measureTimeMillis


private val log = KotlinLogging.logger {}

class UpdateFarmersJob : Job {
    override fun execute(context: JobExecutionContext?) {
        val farmers = transaction { Farmer.all().toList() }
        if (farmers.isEmpty()) {
            log.info { "No farmers to update…" }
            return
        }

        log.info { "Updating farmers…" }

        val timeTaken = measureTimeMillis {
            runBlocking {
                farmers.map { GlobalScope.async { it.update() } }.awaitAll()
            }
        }

        log.info { "Finished updating farmers in ${timeTaken}ms" }
    }
}
