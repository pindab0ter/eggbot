package nl.pindab0ter.eggbot.jobs

import mu.KotlinLogging
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.Messages
import nl.pindab0ter.eggbot.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.Job
import org.quartz.JobExecutionContext

class UpdateLeaderBoardsJob : Job {

    private val log = KotlinLogging.logger {}

    override fun execute(context: JobExecutionContext?) {
        UpdateFarmersJob().execute(context)

        val farmers = transaction {
            Farmer.all().toList()
        }

        if (farmers.isEmpty()) {
            log.info { "There are no registered farmers" }
            return
        }

        updateLeaderBoard(
            "Earnings Bonus",
            Config.earningsBonusLeaderBoardChannel,
            farmers.sortedByDescending { it.earningsBonus },
            Messages::earningsBonusLeaderBoard
        )

        updateLeaderBoard(
            "Soul Eggs",
            Config.soulEggsLeaderBoardChannel,
            farmers.sortedByDescending { it.soulEggs },
            Messages::soulEggsLeaderBoard
        )

        updateLeaderBoard(
            "Prestiges",
            Config.prestigesLeaderBoardChannel,
            farmers.sortedByDescending { it.prestiges },
            Messages::prestigesLeaderBoard
        )

        updateLeaderBoard(
            "Drone Takedowns",
            Config.droneTakedownsLeaderBoardChannel,
            farmers.sortedByDescending { it.droneTakedowns },
            Messages::droneTakedownsLeaderBoard
        )

        updateLeaderBoard(
            "Elite Drone Takedowns",
            Config.eliteDroneTakedownsLeaderBoardChannel,
            farmers.sortedByDescending { it.eliteDroneTakedowns },
            Messages::eliteDroneTakedownsLeaderBoard
        )
    }

    private fun updateLeaderBoard(
        title: String,
        channel: String,
        sortedFarmers: List<Farmer>,
        messageFunction: (List<Farmer>) -> List<String>
    ) = EggBot.jdaClient.getTextChannelById(channel)?.apply {
        log.info { "Updating $title leader board…" }

        history.retrievePast(100).complete().let {
            purgeMessages(it)
        }

        messageFunction(sortedFarmers).forEach { message ->
            sendMessage(message).queue()
        }
    }
}