package nl.pindab0ter.eggbot

import discord4j.core.DiscordClientBuilder
import discord4j.core.`object`.entity.Message
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("usage: EggBot bot_token")
    }

    prepareDatabase()

    transaction {
        if (Farmers.select { Farmers.inGameName eq "pindab0ter" }.empty()) {
            Farmers.insert {
                it[inGameName] = "pindab0ter"
                it[discordName] = "pindab0ter#5483"
                it[role] = Roles.TeraFarmer
            }
        }
    }

    val client = DiscordClientBuilder(args[0]).build()

    client.eventDispatcher.on(ReadyEvent::class.java)
        .subscribe { ready -> println("Logged in as ${ready.self.username}") }

    client.eventDispatcher.on(MessageCreateEvent::class.java)
        .map(MessageCreateEvent::getMessage)
        .filter { msg -> msg.content.map("!ping"::equals).orElse(false) }
        .flatMap(Message::getChannel)
        .flatMap { channel -> channel.createMessage("Pong!") }
        .subscribe()

    client.login().block()
}

fun prepareDatabase() {
    Database.connect("jdbc:sqlite:./EggBot.sqlite", driver = "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

    transaction {
        SchemaUtils.create(Coops)
        SchemaUtils.create(Farmers)
        SchemaUtils.create(Contracts)
        SchemaUtils.create(Tiers)
    }
}
