package nl.pindab0ter.eggbot

import com.jagrosh.jdautilities.command.CommandClientBuilder
import net.dv8tion.jda.core.JDABuilder
import nl.pindab0ter.eggbot.commands.ContractIDs
import nl.pindab0ter.eggbot.commands.LeaderBoard
import nl.pindab0ter.eggbot.commands.Register
import nl.pindab0ter.eggbot.commands.RollCall
import nl.pindab0ter.eggbot.database.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.FileInputStream
import java.sql.Connection
import java.util.*

fun main() = with(EggBot) {
    connectToDatabase()
    initializeDatabase()
    connectClient()
}


object EggBot {
    private val botToken: String
    private val ownerId: String

    init {
        Properties().apply {
            load(FileInputStream("config.properties"))
            botToken = getProperty("bot_token")
            ownerId = getProperty("owner_id")
        }
    }

    fun initializeDatabase() = transaction {
        SchemaUtils.create(DiscordUsers)
        SchemaUtils.create(Farmers)
        SchemaUtils.create(Coops)
        SchemaUtils.create(CoopFarmers)
    }

    fun connectToDatabase() {
        Database.connect("jdbc:sqlite:./EggBot.sqlite", driver = "org.sqlite.JDBC")
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    }

    fun connectClient() {
        requireNotNull(botToken) { "Please enter the bot token in the \"bot_token\" environment variable" }
        requireNotNull(ownerId) { "Please enter the owner id in the \"owner_id\" environment variable" }

        val client = CommandClientBuilder()
            .setOwnerId(ownerId)
            .setPrefix("!")
            // TODO: Customize help message; remove "For additional help[...]"; add aliases
            .useHelpBuilder(true)
            .addCommands(
                ContractIDs,
                LeaderBoard,
                Register,
                RollCall
            )
            // TODO: Specify allowed server and roles
            .build()

        JDABuilder(botToken)
            .addEventListener(client)
            .build()
            .awaitReady()
    }

    fun clearDatabase() {
        transaction {
            DiscordUser.all().forEach(DiscordUser::delete)
            Farmer.all().forEach(Farmer::delete)
        }
    }
}

