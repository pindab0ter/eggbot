package nl.pindab0ter.eggbot

import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import nl.pindab0ter.eggbot.commands.commands

class MessageListener : ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent?) {
        if (event?.message?.isCommand == true) commands
            .find { it.keyWord.toLowerCase() == event.message.command }
            ?.execute(event)
    }
}