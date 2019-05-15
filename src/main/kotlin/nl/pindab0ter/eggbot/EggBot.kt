package nl.pindab0ter.eggbot

import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Guild
import nl.pindab0ter.eggbot.database.*
import nl.pindab0ter.eggbot.jda.CommandLogger
import nl.pindab0ter.eggbot.jda.EyeReaction
import nl.pindab0ter.eggbot.jda.commandClient
import nl.pindab0ter.eggbot.jobs.JobLogger
import nl.pindab0ter.eggbot.jobs.UpdateFarmersJob
import nl.pindab0ter.eggbot.jobs.UpdateLeaderBoardsJob
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
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
        SchemaUtils.create(Contracts)
        SchemaUtils.create(Goals)

        if (Config.devMode) {
            Goals.deleteAll()
            Contracts.deleteAll()
            Coops.deleteAll()
            CoopFarmers.deleteAll()
        }

    }

    private fun connectToDatabase() {
        Database.connect("jdbc:sqlite:./EggBot.sqlite", driver = "org.sqlite.JDBC")
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
                .withIdentity("quarter_daily")
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

    val guild: Guild get() = jdaClient.guilds.first()
}
