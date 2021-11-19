package nl.pindab0ter.eggbot.jobs

import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.helpers.asyncMap
import nl.pindab0ter.eggbot.model.database.DiscordUser
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.Job
import org.quartz.JobExecutionContext
import kotlin.system.measureTimeMillis


class UpdateDiscordTagsJob : Job {

    @OptIn(PrivilegedIntent::class)
    override fun execute(context: JobExecutionContext?) {
        val discordUsers = transaction { DiscordUser.all().toList() }

        val timeTakenMillis = measureTimeMillis {
            runBlocking {
                discordUsers.asyncMap { discordUser ->
                    transaction { launch { discordUser.updateTag() } }
                }
            }
        }

        logger.info { "Updated Discord user’s tags in ${timeTakenMillis}ms" }
    }

    companion object {
        val logger = KotlinLogging.logger { }
    }
}
