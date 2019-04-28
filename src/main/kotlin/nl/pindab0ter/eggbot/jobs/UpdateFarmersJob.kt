package nl.pindab0ter.eggbot.jobs

import mu.KotlinLogging
import nl.pindab0ter.eggbot.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.Job
import org.quartz.JobExecutionContext
import kotlin.system.measureTimeMillis


class UpdateFarmersJob : Job {

    private val log = KotlinLogging.logger {}

    override fun execute(context: JobExecutionContext?) {
        val farmers = transaction { Farmer.all().toList() }
        if (farmers.isEmpty()) {
            log.info { "No farmers to update…" }
            return
        }

        log.info { "Updating farmers…" }

        val timeTaken = measureTimeMillis {
            farmers.forEach { it.update() }
        }

        log.info { "Finished updating farmers in ${timeTaken}ms" }
    }
}
