package nl.pindab0ter.eggbot

import net.dv8tion.jda.core.JDABuilder
import nl.pindab0ter.eggbot.commands.Command
import nl.pindab0ter.eggbot.commands.Contracts
import nl.pindab0ter.eggbot.commands.Help
import nl.pindab0ter.eggbot.commands.Register
import nl.pindab0ter.eggbot.database.prepareDatabase
import nl.pindab0ter.eggbot.network.getContracts


fun main() {
    System.getenv()["bot_token"]?.let { botToken ->
        JDABuilder(botToken)
            .addEventListener(MessageListener())
            .build()
            .awaitReady()
    } ?: run {
        throw MissingEnvironmentVariableException("Please enter the bot token in the \"bot_token\" environment variable")
    }

    prepareDatabase()

    getContracts()
}
