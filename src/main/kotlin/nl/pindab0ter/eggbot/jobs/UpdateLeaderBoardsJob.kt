package nl.pindab0ter.eggbot.jobs

import mu.KotlinLogging
import nl.pindab0ter.eggbot.model.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.Job
import org.quartz.JobExecutionContext

class UpdateLeaderBoardsJob : Job {
    override fun execute(context: JobExecutionContext?) {
        UpdateFarmers().execute(context)

        val farmers = transaction {
            Farmer.all().toList()
        }

        if (farmers.isEmpty()) {
            logger.info { "There are no registered farmers" }
            return
        }

        logger.info { "Updating leader boards…" }

        // TODO:
        // listOf(
        //     earningsBonusLeaderBoardChannel to EARNINGS_BONUS,
        //     soulEggsLeaderBoardChannel to SOUL_EGGS,
        //     prestigesLeaderBoardChannel to PRESTIGES,
        //     dronesLeaderBoardChannel to DRONE_TAKEDOWNS,
        //     eliteDronesLeaderBoardChannel to ELITE_DRONE_TAKEDOWNS
        // ).forEach { (channel, category) ->
        //     channel.history.retrievePast(100).complete().let { messages ->
        //         channel.purgeMessages(messages)
        //     }
        //
        //     LeaderBoard.formatLeaderBoard(
        //         farmers,
        //         category,
        //         top = null,
        //         compact = false,
        //         extended = true
        //     ).forEach { message ->
        //         channel.sendMessage(message).queue()
        //     }
        // }
    }

    companion object {
        val logger = KotlinLogging.logger { }
    }
}