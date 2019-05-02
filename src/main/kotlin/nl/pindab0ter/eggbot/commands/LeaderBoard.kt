package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.Messages
import nl.pindab0ter.eggbot.arguments
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.replyInDms
import org.jetbrains.exposed.sql.transactions.transaction
import sun.plugin2.message.Message

object LeaderBoard : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "leader-board"
        aliases = arrayOf("lb", "leaderboard")
        help = "Shows the Earnings Bonus leader board"
        arguments = "[compact]"
        // category = LeaderBoardsCategory
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        if (event.channel.id == Config.botCommandsChannel) {
            event.channel.sendTyping()
        } else {
            event.author.openPrivateChannel().queue { it.sendTyping().complete() }
        }

        val farmers = transaction {
            Farmer.all().toList().sortedByDescending { it.earningsBonus }
        }

        if (farmers.isEmpty()) "There are no registered farmers".let {
            event.replyWarning(it)
            log.trace { it }
            return
        }

        (if (event.arguments.isNotEmpty()) Messages::earningsBonusLeaderBoardCompact
        else Messages::earningsBonusLeaderBoard).invoke(farmers).let { messages ->
            if (event.channel.id == Config.botCommandsChannel) {
                messages.forEach { message -> event.reply(message) }
            } else {
                event.replyInDms(messages)
            }
        }
    }
}
