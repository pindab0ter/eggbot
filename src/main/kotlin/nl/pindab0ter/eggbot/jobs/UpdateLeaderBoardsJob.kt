package nl.pindab0ter.eggbot.jobs

import mu.KotlinLogging
import nl.pindab0ter.eggbot.EggBot.dronesLeaderBoardChannel
import nl.pindab0ter.eggbot.EggBot.earningsBonusLeaderBoardChannel
import nl.pindab0ter.eggbot.EggBot.eliteDronesLeaderBoardChannel
import nl.pindab0ter.eggbot.EggBot.prestigesLeaderBoardChannel
import nl.pindab0ter.eggbot.EggBot.soulEggsLeaderBoardChannel
import nl.pindab0ter.eggbot.controller.LeaderBoard
import nl.pindab0ter.eggbot.controller.LeaderBoard.Board.*
import nl.pindab0ter.eggbot.model.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.Job
import org.quartz.JobExecutionContext

class UpdateLeaderBoardsJob : Job {

    private val log = KotlinLogging.logger {}

    override fun execute(context: JobExecutionContext?) {
        UpdateFarmers().execute(context)

        val farmers = transaction {
            Farmer.all().toList()
        }

        if (farmers.isEmpty()) {
            log.info { "There are no registered farmers" }
            return
        }

        log.info { "Updating leader boards…" }

        listOf(
            earningsBonusLeaderBoardChannel to EARNINGS_BONUS,
            soulEggsLeaderBoardChannel to SOUL_EGGS,
            prestigesLeaderBoardChannel to PRESTIGES,
            dronesLeaderBoardChannel to DRONE_TAKEDOWNS,
            eliteDronesLeaderBoardChannel to ELITE_DRONE_TAKEDOWNS
        ).forEach { (channel, category) ->
            channel.history.retrievePast(100).complete().let { messages ->
                channel.purgeMessages(messages)
            }

            LeaderBoard.formatLeaderBoard(
                farmers,
                category,
                top = null,
                compact = false,
                extended = true
            ).forEach { message ->
                channel.sendMessage(message).queue()
            }
        }
    }
}