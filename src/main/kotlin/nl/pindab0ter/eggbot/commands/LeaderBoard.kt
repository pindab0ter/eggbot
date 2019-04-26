package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import nl.pindab0ter.eggbot.commands.categories.LeaderBoardsCategory
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.leaderBoard
import nl.pindab0ter.eggbot.replyInDms
import org.jetbrains.exposed.sql.transactions.transaction

object LeaderBoard : Command() {
    init {
        name = "leader-board"
        aliases = arrayOf("lb", "leaderboard")
        help = "Shows the Earnings Bonus leader board"
        category = LeaderBoardsCategory
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

        event.replyInDms(leaderBoard(farmers))
    }
}
