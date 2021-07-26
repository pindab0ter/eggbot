package nl.pindab0ter.eggbot.jobs

import dev.kord.core.Kord
import dev.kord.gateway.PrivilegedIntent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.model.database.DiscordUser
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.java.KoinJavaComponent.inject
import org.quartz.Job
import org.quartz.JobExecutionContext


class UpdateDiscordTagsJob : Job {
    val kord: Kord by inject(Kord::class.java)

    @OptIn(PrivilegedIntent::class)
    override fun execute(context: JobExecutionContext?): Unit = transaction {
        logger.info { "Updating Discord user's tags…" }
        DiscordUser.all().toList().forEach { discordUser -> discordUser.updateTag() }
    }

    companion object {
        val logger = KotlinLogging.logger { }
    }
}
