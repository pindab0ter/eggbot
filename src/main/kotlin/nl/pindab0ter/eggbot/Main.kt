package nl.pindab0ter.eggbot

import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

const val prefix = "!"
val commands: List<Command> = listOf(
    Help,
    Register
)

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("usage: EggBot bot_token")
    }

    prepareDatabase()

    JDABuilder(args[0])
        .addEventListener(MessageListener())
        .build()
        .awaitReady()
}

class MessageListener : ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent?) {
        if (event?.message?.isCommand == true) commands
            .find { it.keyWord.toLowerCase() == event.message.command }
            ?.function(event)
    }
}
