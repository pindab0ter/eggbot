package nl.pindab0ter.eggbot

import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Guild
import nl.pindab0ter.eggbot.database.CoopFarmers
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.database.DiscordUsers
import nl.pindab0ter.eggbot.database.Farmers
import nl.pindab0ter.eggbot.jda.CommandLogger
import nl.pindab0ter.eggbot.jda.EyeReaction
import nl.pindab0ter.eggbot.jda.commandClient
import nl.pindab0ter.eggbot.jobs.JobLogger
import nl.pindab0ter.eggbot.jobs.UpdateDiscordTagsJob
import nl.pindab0ter.eggbot.jobs.UpdateFarmersJob
import nl.pindab0ter.eggbot.jobs.UpdateLeaderBoardsJob
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.CronScheduleBuilder.weeklyOnDayAndHourAndMinute
import org.quartz.DateBuilder.FRIDAY
import org.quartz.JobBuilder.newJob
import org.quartz.SimpleScheduleBuilder.simpleSchedule
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.impl.StdSchedulerFactory
import java.sql.Connection
import java.time.ZoneId
import java.util.*


object EggBot {
    val jdaClient: JDA = JDABuilder(Config.botToken)
        .addEventListener(CommandLogger)
        .addEventListener(EyeReaction)
        .addEventListener(commandClient)
        .build()

    @JvmStatic
    fun main(args: Array<String>) {
        connectToDatabase()
        initializeDatabase()
        startScheduler()
        jdaClient.awaitReady()
    }

    private fun initializeDatabase() = transaction {
        SchemaUtils.create(DiscordUsers)
        SchemaUtils.create(Farmers)
        SchemaUtils.create(Coops)
        SchemaUtils.create(CoopFarmers)
    }

    private fun connectToDatabase() {
        Database.connect(
            url = "jdbc:sqlite:./EggBot.sqlite",
            driver = "org.sqlite.JDBC",
            setupConnection = { connection ->
                connection.createStatement().execute("PRAGMA foreign_keys = ON")
            })
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    }

    private fun startScheduler() = StdSchedulerFactory.getDefaultScheduler().apply {
        // Use Europe/London because it moves with Daylight Saving Time
        val london = TimeZone.getTimeZone(ZoneId.of("Europe/London"))

        if (!Config.devMode) scheduleJob(
            newJob(UpdateFarmersJob::class.java)
                .withIdentity("update_farmers")
                .build(),
            newTrigger()
                .withIdentity("quarter_daily_farmer_update")
                .withSchedule(simpleSchedule().withIntervalInHours(6).repeatForever())
                .build()
        )
        if (!Config.devMode) scheduleJob(
            newJob(UpdateDiscordTagsJob::class.java)
                .withIdentity("update_discord_tags")
                .build(),
            newTrigger()
                .withIdentity("quarter_daily_discord_tags_update")
                .withSchedule(simpleSchedule().withIntervalInHours(6).repeatForever())
                .build()
        )
        scheduleJob(
            newJob(UpdateLeaderBoardsJob::class.java)
                .withIdentity("update_leader_board")
                .build(),
            newTrigger()
                .withIdentity("every_friday_at_noon")
                .withSchedule(
                    weeklyOnDayAndHourAndMinute(FRIDAY, 12, 0).inTimeZone(london)
                )
                .build()
        )
        listenerManager.addJobListener(JobLogger)
        start()
    }

    val guild: Guild by lazy { jdaClient.guilds.first() }
}
