package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jdautilities.command.CommandEvent.splitMessage
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.format
import org.jetbrains.exposed.sql.transactions.transaction

object LeaderBoard : Command() {
    init {
        name = "lb"
        aliases = arrayOf("leaderboard", "leader-board")
        help = "Shows the Earnings Bonus leader board"
        // TODO: Make guild only
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        val farmers = transaction {
            Farmer.all().toList().sortedByDescending { it.earningsBonus }
        }

        // Make a list of strings, each for a leader board entry, then count how long they are and fit accordingly
        splitMessage(StringBuilder().appendln().apply {
            val farmersCountLength = farmers.count().toString().length
            val longestFarmerName = farmers.maxBy { it.inGameName.length }!!.inGameName.length
            val longestEarningsBonus = format(farmers.maxBy { format(it.earningsBonus).length }!!.earningsBonus).length

            farmers.forEachIndexed { index, farmer ->
                append("`")
                append("${index + 1}:")
                append(" ".repeat(farmersCountLength - (index + 1).toString().length))
                append(" ")
                append(farmer.inGameName)
                append(" ".repeat(longestFarmerName - farmer.inGameName.length))
                append(" ")
                append(" ".repeat(longestEarningsBonus - format(farmer.earningsBonus).length))
                append(format(farmer.earningsBonus))
                append("`")
                appendln()
            }
        }.toString()).forEachIndexed { i, message ->
            event.reply("${if (i == 0) "Earnings Bonus leader board:" else "Leader board continued…"}\n$message")
        }


        if (farmers.isNotEmpty()) event.reply("")
        else {
            event.replyWarning("There are no registered farmers")
        }
    }
}
