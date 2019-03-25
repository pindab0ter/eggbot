package nl.pindab0ter.eggbot

import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

const val prefix = "!"
val commands: List<Command> = listOf(
    Help,
    PingPong,
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
        if (event?.message?.isCommand == true) {
            commands.find { it.keyWord == event.message.command }
                ?.function(event)
        }
    }
}

fun prepareDatabase() {
    Database.connect("jdbc:sqlite:./EggBot.sqlite", driver = "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    TransactionManager.manager.defaultRepetitionAttempts = 0

    transaction {
        SchemaUtils.create(Coops)
        SchemaUtils.create(Farmers)
        SchemaUtils.create(Contracts)
        SchemaUtils.create(Goals)
    }
}
