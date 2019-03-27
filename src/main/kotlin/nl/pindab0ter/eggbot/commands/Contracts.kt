package nl.pindab0ter.eggbot.commands

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

object Contracts : Command {
    override val keyWord = "contracts"
    override val help = "$PREFIX$keyWord - Shows currently active contracts"

    override fun execute(event: MessageReceivedEvent) = TODO()
}