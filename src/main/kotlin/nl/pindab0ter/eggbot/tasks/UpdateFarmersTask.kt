package nl.pindab0ter.eggbot.tasks

import nl.pindab0ter.eggbot.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*


object UpdateFarmersTask : TimerTask() {
    private val log = KotlinLogging.logger {}
    override fun run() = transaction {
        Farmer.all().sortedBy { it.lastUpdated }
            .let { it.take((it.size / 6) + 1) }
            .forEach(Farmer::update)
    }
}
