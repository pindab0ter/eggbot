package nl.pindab0ter.eggbot

import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.Connection

internal fun connectToDatabase() {
    val logger = KotlinLogging.logger("Exposed")
    Database.connect(
        url = "jdbc:sqlite:./EggBot.sqlite",
        driver = "org.sqlite.JDBC",
        setupConnection = { connection ->
            connection.createStatement().execute("PRAGMA foreign_keys = ON")
        })
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    logger.info { "Connected to database" }
}
