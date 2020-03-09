package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import mu.KotlinLogging
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.EggBot.botCommandsChannel
import nl.pindab0ter.eggbot.Messages
import nl.pindab0ter.eggbot.commands.categories.FarmersCategory
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.utilities.replyInDms
import org.jetbrains.exposed.sql.transactions.transaction

object LeaderBoard : EggBotCommand() {

    private val log = KotlinLogging.logger { }

    init {
        category = FarmersCategory
        name = "leader-board"
        aliases = arrayOf("lb")
        help = "Shows the Earnings Bonus leader board"
        parameters = listOf(
            compactSwitch
        )
        init()
    }

    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val farmers = transaction {
            Farmer.all().toList().sortedByDescending { it.earningsBonus }
        }

        if (farmers.isEmpty()) "There are no registered farmers".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        (if (parameters.getBoolean(COMPACT)) Messages::earningsBonusLeaderBoardCompact
        else Messages::earningsBonusLeaderBoard).invoke(farmers).let { messages ->
            if (event.channel == botCommandsChannel) {
                messages.forEach { message -> event.reply(message) }
            } else {
                event.replyInDms(messages)
            }
        }
    }
}
