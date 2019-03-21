package nl.pindab0ter.eggbot

import discord4j.core.DiscordClientBuilder
import discord4j.core.`object`.entity.Message
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.event.domain.message.MessageCreateEvent

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("usage: EggBot bot_token")
    }

    val client = DiscordClientBuilder(args[0]).build()
    client.eventDispatcher.on(ReadyEvent::class.java)
        .subscribe { ready -> println("Logged in as ${ready.self.username}") }

    client.eventDispatcher.on(MessageCreateEvent::class.java)
        .map(MessageCreateEvent::getMessage)
        .filter { msg -> msg.content.map("!ping"::equals).orElse(false) }
        .flatMap(Message::getChannel)
        .flatMap{channel -> channel.createMessage("Pong!")}
        .subscribe()

    client.login().block()
}
