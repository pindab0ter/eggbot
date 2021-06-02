package nl.pindab0ter.eggbot.jobs

import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.Job
import org.quartz.JobExecutionContext


class UpdateDiscordTagsJob : Job {
    override fun execute(context: JobExecutionContext?): Unit = transaction {
        logger.info { "Updating Discord user's tags…" }
        // TODO:
        // DiscordUser.all().toList().forEach { discordUser -> discordUser.updateTag() }
    }

    companion object {
        val logger = KotlinLogging.logger { }
    }
}
