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
        val farmers = transaction {
            Farmer.all().toList()
        }

        if (farmers.isEmpty()) {
            log.info { "There are no registered farmers" }
            return
        }

        EggBot.jdaClient.getTextChannelById(Config.earningsBonusLeaderBoardChannel).apply {
            history.retrievePast(100).complete().let { messages ->
                log.info { "Purging ${messages.count()} Earnings Bonus leader board messages…" }
                purgeMessages(messages)
            }

            log.info { "Sending updated Earnings Bonus leader board…" }
            Messages.earningsBonusLeaderBoard(farmers.sortedByDescending { it.earningsBonus }).forEach { message ->
                sendMessage(message).queue()
            }
        }

        EggBot.jdaClient.getTextChannelById(Config.soulEggsLeaderBoardChannel).apply {
            history.retrievePast(100).complete().let { messages ->
                log.info { "Purging ${messages.count()} Soul Eggs leader board messages…" }
                purgeMessages(messages)
            }

            log.info { "Sending updated Soul Eggs leader board…" }
            Messages.soulEggsLeaderBoard(farmers.sortedByDescending { it.soulEggs }).forEach { message ->
                sendMessage(message).queue()
            }
        }
    }
}