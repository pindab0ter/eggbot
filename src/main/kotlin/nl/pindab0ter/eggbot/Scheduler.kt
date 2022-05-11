package nl.pindab0ter.eggbot

import mu.KotlinLogging
import nl.pindab0ter.eggbot.jobs.JobLogger
import nl.pindab0ter.eggbot.jobs.UpdateLeaderBoardsJob
import org.quartz.CronScheduleBuilder.weeklyOnDayAndHourAndMinute
import org.quartz.JobBuilder.newJob
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.impl.StdSchedulerFactory
import java.time.ZoneId
import java.util.*
import java.util.Calendar.FRIDAY

internal fun startScheduler() = StdSchedulerFactory.getDefaultScheduler().apply {
    val logger = KotlinLogging.logger {}
    // Use Europe/London because it moves with Daylight Saving Time
    val london = TimeZone.getTimeZone(ZoneId.of("Europe/London"))

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