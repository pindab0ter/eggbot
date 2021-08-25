package nl.pindab0ter.eggbot.jobs

import mu.KotlinLogging
import nl.pindab0ter.eggbot.helpers.formatDayHourAndMinutes
import org.joda.time.DateTime
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.quartz.JobListener

object JobLogger : JobListener {

    val logger = KotlinLogging.logger { }

    override fun jobToBeExecuted(context: JobExecutionContext?) = logger.info {
        "Executing job ${context?.jobDetail?.key?.name}…"
    }

    override fun jobExecutionVetoed(context: JobExecutionContext?) = Unit

    override fun getName(): String = "job_logger"

    override fun jobWasExecuted(context: JobExecutionContext?, jobException: JobExecutionException?) = logger.info {
        "Finished job ${context?.jobDetail?.key?.name} in ${context?.jobRunTime}ms. Next run: ${
            context?.nextFireTime?.let { next ->
                DateTime(next.time).formatDayHourAndMinutes()
            }
        }"
    }
}