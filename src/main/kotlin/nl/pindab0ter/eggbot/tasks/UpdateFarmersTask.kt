package nl.pindab0ter.eggbot.tasks

import mu.KotlinLogging
import nl.pindab0ter.eggbot.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*


object UpdateFarmersTask : TimerTask() {
    private val log = KotlinLogging.logger {}
    override fun run(): Unit = transaction {
        Farmer.all().sortedBy { it.lastUpdated }
            .takeIf { it.isNotEmpty() }
            ?.let { it.take((it.size / 6) + 1) }
            ?.also { log.info("Updating ${it.joinToString { farmer -> farmer.inGameName }}") }
            ?.forEach(Farmer::update)
    }
}
