package nl.pindab0ter.eggbot.jobs

import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.supplier.EntitySupplyStrategy.Companion.rest
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.helpers.guild
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.Config.dronesLeaderBoardChannel
import nl.pindab0ter.eggbot.model.Config.earningsBonusLeaderBoardChannel
import nl.pindab0ter.eggbot.model.Config.eliteDronesLeaderBoardChannel
import nl.pindab0ter.eggbot.model.Config.prestigesLeaderBoardChannel
import nl.pindab0ter.eggbot.model.Config.soulEggsLeaderBoardChannel
import nl.pindab0ter.eggbot.model.LeaderBoard.*
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.view.leaderboardResponse
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.Job
import org.quartz.JobExecutionContext

class UpdateLeaderBoardsJob : Job {
    override fun execute(context: JobExecutionContext?) = runBlocking {
        val farmers = transaction {
            Farmer.all().mapNotNull { farmer -> AuxBrain.getFarmerBackup(farmer.eggIncId) }
        }

        if (farmers.isEmpty()) {
            logger.info { "There are no registered farmers" }
            return@runBlocking
        }

        logger.info { "Updating leader boards…" }

        listOf(
            earningsBonusLeaderBoardChannel to EARNINGS_BONUS,
            soulEggsLeaderBoardChannel to SOUL_EGGS,
            prestigesLeaderBoardChannel to PRESTIGES,
            dronesLeaderBoardChannel to DRONE_TAKEDOWNS,
            eliteDronesLeaderBoardChannel to ELITE_DRONE_TAKEDOWNS
        ).map { (channelSnowFlake, category) ->
            guild?.getChannelOfOrNull<TextChannel>(channelSnowFlake) to category
        }.forEach { (textChannel, category) ->
            textChannel!!.withStrategy(rest).messages.collect { message ->
                message.delete("Updating leader boards")
            }

            leaderboardResponse(farmers, category).forEach { content ->
                textChannel.createMessage(content)
            }
        }
    }

    companion object {
        val logger = KotlinLogging.logger { }
    }
}