package nl.pindab0ter.eggbot.jobs

import nl.pindab0ter.eggbot.model.database.DiscordUser
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.Job
import org.quartz.JobExecutionContext


class UpdateDiscordTagsJob : Job, Logging {

    override fun execute(context: JobExecutionContext?): Unit = transaction {
        logger.info { "Updating Discord user's tags…" }
        DiscordUser.all().toList().forEach { discordUser -> discordUser.updateTag() }
    }
}
