package nl.pindab0ter.eggbot.kord.commands

import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalInt
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType.PUBLIC
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommand
import com.kotlindiscord.kord.extensions.commands.slash.converters.ChoiceEnum
import com.kotlindiscord.kord.extensions.commands.slash.converters.impl.defaultingEnumChoice
import com.kotlindiscord.kord.extensions.commands.slash.converters.impl.optionalEnumChoice
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.helpers.DisplayMode
import nl.pindab0ter.eggbot.helpers.publicMultipartFollowUp
import nl.pindab0ter.eggbot.kord.commands.LeaderBoard.Board.EARNINGS_BONUS
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.view.leaderboardResponse
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

@KordPreview
object LeaderBoard {
    enum class Board : ChoiceEnum {
        EARNINGS_BONUS, SOUL_EGGS, PROPHECY_EGGS, PRESTIGES, DRONE_TAKEDOWNS, ELITE_DRONE_TAKEDOWNS;

        override val readableName: String
            get() = name.split("_").joinToString(" ") { word ->
                word.lowercase(Locale.getDefault()).replaceFirstChar { letter -> letter.titlecase(Locale.getDefault()) }
            }
    }

    class LeaderBoardArguments : Arguments() {
        val top: Int? by optionalInt(
            displayName = "top",
            description = "How many players to show",
        )
        val board: Board by defaultingEnumChoice(
            displayName = "board",
            description = "Which board to show",
            defaultValue = EARNINGS_BONUS,
            typeName = Board::name.name,
        )
        val displayMode: DisplayMode? by optionalEnumChoice(
            displayName = "displaymode",
            description = "Use compact to better fit mobile devices or extended to show numbers in non-scientific notation.",
            typeName = DisplayMode::name.name,
        )
    }

    val command: suspend SlashCommand<out LeaderBoardArguments>.() -> Unit = {
        name = "leader-board"
        description = "View leader boards. Defaults to the Earnings Bonus leader board."
        autoAck = PUBLIC

        lateinit var farmers: List<Farmer>

        check {
            farmers = transaction { Farmer.all().toList().sortedByDescending { it.earningsBonus } }
            failIf("There are no registered farmers.") { farmers.isEmpty() }
        }

        action {
            publicMultipartFollowUp(leaderboardResponse(
                farmers = farmers,
                board = arguments.board,
                top = arguments.top?.takeIf { it > 0 },
                displayMode = arguments.displayMode,
                context = this,
            ))
        }
    }
}