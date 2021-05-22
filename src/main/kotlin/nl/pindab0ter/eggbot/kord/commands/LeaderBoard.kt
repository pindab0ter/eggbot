package nl.pindab0ter.eggbot.kord.commands

import com.kotlindiscord.kord.extensions.commands.converters.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.optionalInt
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType.PUBLIC
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommand
import com.kotlindiscord.kord.extensions.commands.slash.converters.defaultingEnumChoice
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.controller.LeaderBoard
import nl.pindab0ter.eggbot.controller.LeaderBoard.Board.EARNINGS_BONUS
import nl.pindab0ter.eggbot.helpers.publicFollowUp
import nl.pindab0ter.eggbot.model.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction

@KordPreview
object LeaderBoard {

    class LeaderBoardArguments : Arguments() {
        val top: Int? by optionalInt(
            displayName = "top",
            description = "How many players to show",
            outputError = false,
        )
        val board: LeaderBoard.Board by defaultingEnumChoice(
            displayName = "board",
            description = "Which board to show",
            typeName = "leaderBoard",
            defaultValue = EARNINGS_BONUS,
        )
        val compact: Boolean by defaultingBoolean(
            displayName = "compact",
            description = "Use a narrower output to better fit mobile devices",
            defaultValue = false,
        )
    }

    val command: suspend SlashCommand<out LeaderBoardArguments>.() -> Unit = {
        name = "leader-board"
        description = "View leader boards. Defaults to the Earnings Bonus leader board."
        autoAck = PUBLIC

        action {
            val farmers = transaction {
                Farmer.all().toList().sortedByDescending { it.earningsBonus }
            }

            if (farmers.isEmpty()) publicFollowUp {
                content = "There are no registered farmers."
            }

            publicFollowUp(LeaderBoard.formatLeaderBoard(
                farmers = farmers,
                board = arguments.board,
                top = arguments.top?.takeIf { it > 0 },
                compact = arguments.compact,
                extended = false
            ))
        }
    }
}