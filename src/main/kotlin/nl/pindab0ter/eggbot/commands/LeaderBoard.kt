package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.Messages
import nl.pindab0ter.eggbot.commands.categories.FarmersCategory
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.utilities.arguments
import nl.pindab0ter.eggbot.utilities.replyInDms
import org.jetbrains.exposed.sql.transactions.transaction

object LeaderBoard : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "leader-board"
        aliases = arrayOf("lb")
        help = "Shows the Earnings Bonus leader board"
        arguments = "[compact]"
        category = FarmersCategory
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        if (event.channel == EggBot.botCommandsChannel) {
            event.channel.sendTyping()
        } else {
            event.author.openPrivateChannel().queue { it.sendTyping().complete() }
        }

        val farmers = transaction {
            Farmer.all().toList().sortedByDescending { it.earningsBonus }
        }

        if (farmers.isEmpty()) "There are no registered farmers".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        (if (event.arguments.isNotEmpty()) Messages::earningsBonusLeaderBoardCompact
        else Messages::earningsBonusLeaderBoard).invoke(farmers).let { messages ->
            if (event.channel == EggBot.botCommandsChannel) {
                messages.forEach { message -> event.reply(message) }
            } else {
                event.replyInDms(messages)
            }
        }
    }
}
