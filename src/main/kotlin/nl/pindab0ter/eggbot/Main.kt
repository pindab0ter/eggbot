package nl.pindab0ter.eggbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import dev.kord.common.annotation.KordPreview
import mu.KotlinLogging
import nl.pindab0ter.eggbot.kord.extensions.CommandLoggerExtension
import nl.pindab0ter.eggbot.kord.extensions.EggBotExtension
import nl.pindab0ter.eggbot.model.Config
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.Connection

@KordPreview
suspend fun main() {
    connectToDatabase()

    ExtensibleBot(Config.botToken) {
        extensions {
            add(::CommandLoggerExtension)
            add(::EggBotExtension)
        }

        slashCommands {
            enabled = true
            // defaultGuild = Snowflake(Config.guildId)
        }
    }.start()
}

private fun connectToDatabase() {
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

