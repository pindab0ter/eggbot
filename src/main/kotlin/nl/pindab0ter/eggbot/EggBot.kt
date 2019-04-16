package nl.pindab0ter.eggbot

import com.jagrosh.jdautilities.command.CommandClient
import com.jagrosh.jdautilities.command.CommandClientBuilder
import net.dv8tion.jda.core.JDABuilder
import nl.pindab0ter.eggbot.commands.*
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
import java.sql.Connection
import java.util.*


object EggBot {
    val client: CommandClient = CommandClientBuilder()
        .setOwnerId(Config.ownerId)
        .setPrefix(Config.prefix)
        .setHelpWord(Config.helpWord)
        .useHelpBuilder(true)
        .setHelpConsumer(Help)
        .addCommands(
            ContractIDs,
            LeaderBoard,
            Register,
            RollCall
        )
        .setEmojis(
            Config.successEmoji,
            Config.warningEmoji,
            Config.errorEmoji
        )
        // TODO: Specify allowed server and roles
        .build()

    @JvmStatic
    fun main(args: Array<String>) {
        connectToDatabase()
        initializeDatabase()
        if (Config.devMode) clearDatabase()
        if (!Config.devMode) startTimerTasks()
        connectClient()
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
        JDABuilder(Config.botToken)
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

