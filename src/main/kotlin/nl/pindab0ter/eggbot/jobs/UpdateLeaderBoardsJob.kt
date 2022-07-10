package nl.pindab0ter.eggbot.jobs

import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.supplier.EntitySupplyStrategy.Companion.rest
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.config
import nl.pindab0ter.eggbot.databases
import nl.pindab0ter.eggbot.helpers.getChannelOfOrNull
import nl.pindab0ter.eggbot.helpers.kord
import nl.pindab0ter.eggbot.helpers.onEachAsync
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.view.leaderboardResponse
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.Job
import org.quartz.JobExecutionContext

class UpdateLeaderBoardsJob : Job {
    override fun execute(context: JobExecutionContext?) = config.servers.forEach { server ->

        // Update all farmers
        val farmers = transaction(databases[server.name]) {
            runBlocking {
                Farmer.all().onEachAsync { farmer ->
                    AuxBrain.getFarmerBackup(farmer.eggIncId, databases[server.name])
                }.toList()
            }
        }

        if (farmers.isEmpty()) {
            logger.info { "There are no registered farmers" }
            return
        }

        logger.info { "Updating leader boards in ${server.name}…" }

        runBlocking {
            val guild = kord.getGuild(server.snowflake)
            server.configuredLeaderBoards
                .mapValues { (_, channelSnowflake) ->
                    guild?.getChannelOfOrNull<TextChannel>(channelSnowflake)
                }
                .forEach { (category, textChannel) ->
                    textChannel?.withStrategy(rest)?.messages?.collect { message ->
                        message.delete("Updating leader boards")
                    }

                    guild?.leaderboardResponse(farmers, category, server = server)?.forEach { content ->
                        textChannel?.createMessage(content)
                    }
                }
        }
    }

    companion object {
        val logger = KotlinLogging.logger { }
    }
}
