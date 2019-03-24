package nl.pindab0ter.eggbot

import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.EventListener
import net.dv8tion.jda.core.hooks.ListenerAdapter
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.util.*

const val prefix = "!"
val commands: HashMap<String, (MessageReceivedEvent) -> Unit?> = hashMapOf(
    "ping" to ::pingPong
)

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

    JDABuilder(args[0])
        .addEventListener(MessageListener())
        .build()
        .awaitReady()
}

class MessageListener : ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent?) {
        // TODO: Parse message and break up into prefix, command and arguments
        val message = event?.message?.contentDisplay
        if (message == null || !message.startsWith(prefix)) return

        commands[message.drop(1)]?.invoke(event)
    }
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
