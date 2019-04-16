package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jdautilities.command.CommandEvent.splitMessage
import nl.pindab0ter.eggbot.appendPaddingSpaces
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.formatAsEB
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
        splitMessage(StringBuilder().appendln().apply {
            farmers.forEachIndexed { index, farmer ->
                append("`")
                append("${index + 1}:")
                appendPaddingSpaces(index + 1, farmers.count())
                append(" ")
                append(farmer.inGameName)
                appendPaddingSpaces(farmer.inGameName, farmers.map { it.inGameName })
                append(" ")
                appendPaddingSpaces(farmer.earningsBonus.formatAsEB(), farmers.map { it.earningsBonus.formatAsEB() })
                append(farmer.earningsBonus.formatAsEB())
                append("`")
                appendln()
            }
        }.toString()).forEachIndexed { i, message ->
            event.reply("${if (i == 0) "Earnings Bonus leader board:" else "Leader board continued…"}\n$message")
        }
    }
}
