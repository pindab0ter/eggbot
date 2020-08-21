package nl.pindab0ter.eggbot.utilities

import mu.KotlinLogging
import nl.pindab0ter.eggbot.helpers.asDaysHoursAndMinutes
import org.joda.time.DateTime
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.quartz.JobListener

object JobLogger : JobListener {
    val log = KotlinLogging.logger {}

    override fun jobToBeExecuted(context: JobExecutionContext?) =
        log.info { "Executing job ${context?.jobDetail?.key?.name}…" }


    override fun jobExecutionVetoed(context: JobExecutionContext?) = Unit

    override fun getName(): String = "job_logger"

    override fun jobWasExecuted(context: JobExecutionContext?, jobException: JobExecutionException?) =
        log.info {
            "Finished job ${context?.jobDetail?.key?.name} in ${context?.jobRunTime}ms. " +
                    "Next run: ${context?.nextFireTime?.let { DateTime(it.time).asDaysHoursAndMinutes() }}"
        }
}