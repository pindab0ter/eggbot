package nl.pindab0ter.eggbot.tasks

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.system.measureTimeMillis


object UpdateFarmersTask : TimerTask() {
    private val log = KotlinLogging.logger {}
    override fun run() {
        val farmers = transaction { Farmer.all().toList() }
        if (farmers.isEmpty()) {
            log.info("No farmers to update…")
            return
        }

        log.info("Updating farmers…")

        val timeTaken = measureTimeMillis {
            runBlocking {
                farmers.map { GlobalScope.async { it.update() } }.awaitAll()
            }
        }

        log.info("Finished updating farmers in ${timeTaken}ms")
    }
}
