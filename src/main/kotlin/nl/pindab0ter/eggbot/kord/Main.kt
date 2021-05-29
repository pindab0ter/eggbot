package nl.pindab0ter.eggbot.kord

import com.kotlindiscord.kord.extensions.ExtensibleBot
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.kord.extensions.EggBotExtension
import nl.pindab0ter.eggbot.model.Config
import org.apache.logging.log4j.kotlin.logger
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.Connection

@KordPreview
suspend fun main() {
    connectToDatabase()

    ExtensibleBot(Config.botToken) {
        extensions {
            add(::EggBotExtension)
        }

        slashCommands {
            enabled = true
        }
    }.start()
}

private fun connectToDatabase() {
    Database.connect(
        url = "jdbc:sqlite:./EggBot.sqlite",
        driver = "org.sqlite.JDBC",
        setupConnection = { connection ->
            connection.createStatement().execute("PRAGMA foreign_keys = ON")
        })
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    logger("Exposed").info { "Connected to database" }
}

