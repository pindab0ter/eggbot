package nl.pindab0ter.eggbot

import com.kotlindiscord.kord.extensions.utils.botHasPermissions
import dev.kord.common.entity.Permission.*
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
        logger.info { "Checking permissions before starting update_leader_board schedule…" }
        config.servers.forEachAsync { server ->
            val guild = kord.getGuildOrNull(server.snowflake)
            server.configuredLeaderBoards.forEach { (channelName, channelId) ->
                val channel = guild?.getChannelOrNull(channelId)

                if (channel == null) {
                    logger.error { "The \"$channelName\" channel with ID $channelId was not found on server ${server.name}" }
                    exitProcess(1)
                }

                if (!channel.botHasPermissions(ViewChannel, SendMessages, ManageMessages, ReadMessageHistory)) {
                    logger.error {
                        "Bot does not have the required permissions for the \"$channelName\" channel with ID $channelId on server ${server.name}.\n" +
                                "The required permissions are: \"View Channel\", \"Send Messages\", \"Manage Messages\" and \"Read Message History\""
                    }
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