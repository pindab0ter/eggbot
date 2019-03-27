package nl.pindab0ter.eggbot.commands

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

interface Command {
    val keyWord: String
    val help: String
    fun execute(event: MessageReceivedEvent)
}
