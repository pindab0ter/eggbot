package nl.pindab0ter.eggbot

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

fun pingPong(event: MessageReceivedEvent) = event.channel.sendMessage("Pong!")?.queue()