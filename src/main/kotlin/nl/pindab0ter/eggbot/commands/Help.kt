package nl.pindab0ter.eggbot.commands

import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

object Help : Command {
    override val keyWord = "help"
    override val help = "$PREFIX$keyWord - Shows this menu"
    override fun execute(event: MessageReceivedEvent) = event.channel
        .sendMessage(
            EmbedBuilder()
                .setTitle("Available commands")
                .setDescription(commands.joinToString("\n") { it.help })
                .build()
        )
        .queue()
}