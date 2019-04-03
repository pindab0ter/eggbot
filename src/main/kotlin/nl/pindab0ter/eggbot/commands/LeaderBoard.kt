package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import nl.pindab0ter.eggbot.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction

object LeaderBoard : Command() {
    private const val FORMAT = "%,d %%"

    init {
        name = "lb"
        aliases = arrayOf("leaderboard", "leader-board")
        help = "Shows the Earnings Bonus leader board"
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        val farmers = transaction {
            Farmer.all().toList().sortedByDescending { it.earningsBonus }
        }

        if (farmers.isNotEmpty()) event.reply(StringBuilder("Earnings Bonus leader board:").appendln().apply {
            val farmersCountLength = farmers.count().toString().length
            val longestFarmerName = farmers.maxBy { it.inGameName.length }!!.inGameName.length
            val longestEarningsBonus = format(farmers.maxBy { format(it.earningsBonus).length }!!.earningsBonus).length

            append("```")
            farmers.forEachIndexed { index, farmer ->
                append("${index + 1}:")
                append(" ".repeat(farmersCountLength - (index + 1).toString().length))
                append(" ")
                append(farmer.inGameName)
                append(" ".repeat(longestFarmerName - farmer.inGameName.length))
                append(" ")
                append(" ".repeat(longestEarningsBonus - format(farmer.earningsBonus).length))
                append(FORMAT.format(farmer.earningsBonus))
                appendln()
            }
            append("```")
        }.toString())
        else {
            event.replyWarning("There are no registered farmers")
        }
    }

    private fun format(earningsBonus: Long) = "%,d%%".format(earningsBonus)
}
