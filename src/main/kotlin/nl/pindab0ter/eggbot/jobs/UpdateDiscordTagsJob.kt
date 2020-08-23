package nl.pindab0ter.eggbot.jobs

import mu.KotlinLogging
import nl.pindab0ter.eggbot.model.database.DiscordUser
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.Job
import org.quartz.JobExecutionContext


class UpdateDiscordTagsJob : Job {

    private val log = KotlinLogging.logger {}

    override fun execute(context: JobExecutionContext?): Unit = transaction {
        log.info { "Updating Discord user's tags…" }
        DiscordUser.all().toList().forEach { discordUser -> discordUser.updateTag() }
    }
}
