package nl.pindab0ter.eggbot

import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

val commands: List<Command> = listOf(
    Help,
    Register
)

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

class MessageListener : ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent?) {
        if (event?.message?.isCommand == true) commands
            .find { it.keyWord.toLowerCase() == event.message.command }
            ?.function(event)
    }
}
