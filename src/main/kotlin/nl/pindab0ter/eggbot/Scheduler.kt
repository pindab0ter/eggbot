package nl.pindab0ter.eggbot

import com.kotlindiscord.kord.extensions.utils.botHasPermissions
import dev.kord.common.entity.Permission
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.helpers.forEachAsync
import nl.pindab0ter.eggbot.helpers.getChannelOrNull
import nl.pindab0ter.eggbot.helpers.kord
import nl.pindab0ter.eggbot.jobs.JobLogger
import nl.pindab0ter.eggbot.jobs.UpdateLeaderBoardsJob
import org.quartz.CronScheduleBuilder.weeklyOnDayAndHourAndMinute
import org.quartz.JobBuilder.newJob
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.impl.StdSchedulerFactory
import java.time.ZoneId
import java.util.*
import java.util.Calendar.FRIDAY
import kotlin.system.exitProcess

internal fun startScheduler() = StdSchedulerFactory.getDefaultScheduler().apply {
    val logger = KotlinLogging.logger {}
    // Use Europe/London because it moves with Daylight Saving Time
    val london = TimeZone.getTimeZone(ZoneId.of("Europe/London"))

    runBlocking {
        config.servers.forEachAsync { server ->
            val guild = kord.getGuild(server.snowflake)
            server.channel.allConfigured.forEach { (channelName, channelId) ->
                val channel = guild?.getChannelOrNull(channelId)

                if (channel == null) {
                    logger.error { "The \"$channelName\" channel with ID $channelId was not found on server ${server.name}" }
                    exitProcess(1)
                }

                if (!channel.botHasPermissions(Permission.ViewChannel)) {
                    logger.error { "Bot does not have permission to view the \"$channelName\" channel with ID $channelId on server ${server.name}" }
                    exitProcess(1)
                }

                if (!channel.botHasPermissions(Permission.SendMessages)) {
                    logger.error { "Bot does not have permission to send messages in the \"$channelName\" channel with ID $channelId on server ${server.name}" }
                    exitProcess(1)
                }
            }
        }
    }

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

    logger.info { "Scheduler started" }
}