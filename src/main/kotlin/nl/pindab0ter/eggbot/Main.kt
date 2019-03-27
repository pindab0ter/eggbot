package nl.pindab0ter.eggbot

import net.dv8tion.jda.core.JDABuilder
import nl.pindab0ter.eggbot.database.connectToDatabase
import nl.pindab0ter.eggbot.database.initializeDatabase


fun main() {
    connectToDatabase()
    initializeDatabase()
    connectClient()
}

private fun connectClient() {
    System.getenv()["bot_token"]?.let { botToken ->
        JDABuilder(botToken)
            .addEventListener(MessageListener())
            .build()
            .awaitReady()
    } ?: run {
        throw MissingEnvironmentVariableException("Please enter the bot token in the \"bot_token\" environment variable")
    }
}
