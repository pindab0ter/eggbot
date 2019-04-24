package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import nl.pindab0ter.eggbot.appendPaddingSpaces
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.formatForDisplay
import nl.pindab0ter.eggbot.splitMessage
import org.jetbrains.exposed.sql.transactions.transaction

object LeaderBoard : Command() {
    init {
        name = "leader-board"
        aliases = arrayOf("lb", "leaderboard")
        help = "Shows the Earnings Bonus leader board"
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {

        // TODO: Add "force-update" argument?

        val farmers = transaction {
            Farmer.all().toList().sortedByDescending { it.earningsBonus }
        }

        if (farmers.isEmpty()) {
            event.replyWarning("There are no registered farmers")
            return
        }

        // Make a list of strings, each for a leader board entry, then count how long they are and fit accordingly
        StringBuilder("Earnings Bonus leader board:\n").apply {
            append("```")
            farmers.forEachIndexed { index, farmer ->
                append("${index + 1}:")
                appendPaddingSpaces(index + 1, farmers.count())
                append(" ")
                append(farmer.inGameName)
                appendPaddingSpaces(farmer.inGameName, farmers.map { it.inGameName })
                append(" ")
                appendPaddingSpaces(
                    farmer.earningsBonus.formatForDisplay(),
                    farmers.map { it.earningsBonus.formatForDisplay() })
                append(farmer.earningsBonus.formatForDisplay())
                appendln()
            }
        }.toString().splitMessage(prefix = "Leader board continued…\n```", postfix = "```").forEach { message ->
            event.reply(message)
        }
    }
}
