package nl.pindab0ter.eggbot

import com.jagrosh.jdautilities.command.CommandClientBuilder
import mu.KotlinLogging
import net.dv8tion.jda.core.JDABuilder
import nl.pindab0ter.eggbot.commands.ContractIDs
import nl.pindab0ter.eggbot.commands.LeaderBoard
import nl.pindab0ter.eggbot.commands.Register
import nl.pindab0ter.eggbot.commands.RollCall
import nl.pindab0ter.eggbot.database.CoopFarmers
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.database.DiscordUsers
import nl.pindab0ter.eggbot.database.Farmers
import nl.pindab0ter.eggbot.tasks.UpdateFarmersTask
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.Duration
import java.io.FileInputStream
import java.sql.Connection
import java.util.*


object EggBot {
    private const val CONFIG_FILE_NAME = "config.properties"
    private const val CONFIG_BOT_TOKEN = "bot_token"
    private const val CONFIG_OWNER_ID = "owner_id"

    private val logger = KotlinLogging.logger { }
    private val botToken: String
    private val ownerId: String

    @JvmStatic
    fun main(args: Array<String>) {
        connectToDatabase()
        initializeDatabase()
        clearDatabase()
        startTimerTasks()
        connectClient()
    }

    init {
        Properties().apply {
            load(FileInputStream("config.properties"))

            botToken = getProperty(CONFIG_BOT_TOKEN)
                ?: throw PropertyNotFoundException("Could not load \"$CONFIG_BOT_TOKEN\" from \"$CONFIG_FILE_NAME\".")
            ownerId = getProperty(CONFIG_OWNER_ID)
                ?: throw PropertyNotFoundException("Could not load \"$CONFIG_OWNER_ID\" from \"$CONFIG_FILE_NAME\".")
        }
    }

    private fun initializeDatabase() = transaction {
        SchemaUtils.create(DiscordUsers)
        SchemaUtils.create(Farmers)
        SchemaUtils.create(Coops)
        SchemaUtils.create(CoopFarmers)
    }

    private fun connectToDatabase() {
        Database.connect("jdbc:sqlite:./EggBot.sqlite", driver = "org.sqlite.JDBC")
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    }

    private fun startTimerTasks() = Timer(true).apply {
        schedule(UpdateFarmersTask, Duration.standardSeconds(2).millis, Duration.standardDays(1).millis)
    }

    private fun connectClient() {
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

    private fun clearDatabase() {
        transaction {
            CoopFarmers.deleteAll()
            Coops.deleteAll()
        }
    }
}

