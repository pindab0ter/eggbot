package nl.pindab0ter.eggbot.kord.commands

import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalInt
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType.PUBLIC
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommand
import com.kotlindiscord.kord.extensions.commands.slash.converters.ChoiceEnum
import com.kotlindiscord.kord.extensions.commands.slash.converters.impl.defaultingEnumChoice
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.Typography.zwsp
import nl.pindab0ter.eggbot.kord.commands.LeaderBoard.Board.*
import nl.pindab0ter.eggbot.model.Table.AlignedColumn.Alignment.RIGHT
import nl.pindab0ter.eggbot.model.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

@KordPreview
object LeaderBoard {
    enum class Board : ChoiceEnum {
        EARNINGS_BONUS, SOUL_EGGS, PROPHECY_EGGS, PRESTIGES, DRONE_TAKEDOWNS, ELITE_DRONE_TAKEDOWNS;

        override val readableName: String
            get() = name.split("_").joinToString(" ") { word ->
                word.lowercase(Locale.getDefault())
                    .replaceFirstChar { letter ->
                        if (letter.isLowerCase()) letter.titlecase(Locale.getDefault())
                        else letter.toString()
                    }
            }
    }

    private fun formatLeaderBoard(
        farmers: List<Farmer>,
        board: Board,
        top: Int?,
        compact: Boolean,
    ): List<String> = table {
        val sortedFarmers = when (board) {
            EARNINGS_BONUS -> farmers.sortedByDescending { farmer -> farmer.earningsBonus }
            SOUL_EGGS -> farmers.sortedByDescending { farmer -> farmer.soulEggs }
            PROPHECY_EGGS -> farmers.sortedByDescending { farmer -> farmer.prophecyEggs }
            PRESTIGES -> farmers.sortedByDescending { farmer -> farmer.prestiges }
            DRONE_TAKEDOWNS -> farmers.sortedByDescending { farmer -> farmer.droneTakedowns }
            ELITE_DRONE_TAKEDOWNS -> farmers.sortedByDescending { farmer -> farmer.eliteDroneTakedowns }
        }.let { sortedFarmers ->
            if (top != null) sortedFarmers.take(top) else sortedFarmers
        }
        val shortenedNames = sortedFarmers.map { farmer ->
            farmer.inGameName.let { name ->
                if (name.length <= 10) name
                else "${name.substring(0 until 9)}…"
            }
        }

        title = "__**${
            when (board) {
                EARNINGS_BONUS -> "💵 Earnings Bonus"
                SOUL_EGGS -> "Soul Eggs"
                PROPHECY_EGGS -> "Prophecy Eggs"
                // TODO:
                // SOUL_EGGS -> "${emoteSoulEgg?.asMention ?: "🥚"} Soul Eggs"
                // PROPHECY_EGGS -> "${emoteProphecyEgg?.asMention ?: "🥚"} Prophecy Eggs"
                PRESTIGES -> "🥨 Prestiges"
                DRONE_TAKEDOWNS -> "✈🚫 Drone Takedowns"
                ELITE_DRONE_TAKEDOWNS -> "🎖✈🚫 Elite Drone Takedowns"
            }
        }${if (!compact) " Leader Board" else ""}**__"
        displayHeaders = true
        if (compact) incrementColumn() else incrementColumn(":")
        column {
            header = "Name"
            leftPadding = 1
            rightPadding = if (compact) 1 else 2
            cells = if (compact) shortenedNames else sortedFarmers.map { farmer -> farmer.inGameName }
        }
        column {
            header = when (board) {
                EARNINGS_BONUS -> "Earnings Bonus" + if (compact) "" else "  " // Added spacing for percent suffix
                SOUL_EGGS -> "Soul Eggs"
                PROPHECY_EGGS -> "Prophecy Eggs"
                PRESTIGES -> "Prestiges"
                DRONE_TAKEDOWNS -> "Drone Takedowns"
                ELITE_DRONE_TAKEDOWNS -> "Elite Drone Takedowns"
            }
            alignment = RIGHT
            cells = when (board) {
                EARNINGS_BONUS -> sortedFarmers.map { farmer -> farmer.earningsBonus.formatIllions(shortened = true) + if (compact) "" else "$zwsp%" }
                SOUL_EGGS -> sortedFarmers.map { farmer -> farmer.soulEggs.formatIllions(shortened = compact) }
                PROPHECY_EGGS -> sortedFarmers.map { farmer -> farmer.prophecyEggs.formatInteger() }
                PRESTIGES -> sortedFarmers.map { farmer -> farmer.prestiges.formatInteger() }
                DRONE_TAKEDOWNS -> sortedFarmers.map { farmer -> farmer.droneTakedowns.formatInteger() }
                ELITE_DRONE_TAKEDOWNS -> sortedFarmers.map { farmer -> farmer.eliteDroneTakedowns.formatInteger() }
            }
        }
        if (board == EARNINGS_BONUS) column {
            header = if (compact) "Role" else "Farmer Role"
            leftPadding = if (compact) 1 else 2
            cells = sortedFarmers.map { farmer -> farmer.earningsBonus.formatRank(shortened = compact) }
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

            publicFollowUp(formatLeaderBoard(
                farmers = farmers,
                board = arguments.board,
                top = arguments.top?.takeIf { it > 0 },
                compact = arguments.compact,
            ))
        }
    }
}