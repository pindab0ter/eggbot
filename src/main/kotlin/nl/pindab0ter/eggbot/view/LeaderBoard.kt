package nl.pindab0ter.eggbot.view

import com.kotlindiscord.kord.extensions.commands.CommandContext
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.kord.commands.LeaderBoard
import nl.pindab0ter.eggbot.kord.commands.LeaderBoard.Board.*
import nl.pindab0ter.eggbot.model.Config
import nl.pindab0ter.eggbot.model.Table
import nl.pindab0ter.eggbot.model.database.Farmer

@KordPreview
suspend fun leaderboardResponse(
    farmers: List<Farmer>,
    board: LeaderBoard.Board,
    top: Int?,
    compact: Boolean,
    context: CommandContext,
): List<String> = table {
    val sortedFarmers = when (board) {
        EARNINGS_BONUS -> farmers.sortedByDescending { farmer -> farmer.earningsBonus }
        SOUL_EGGS -> farmers.sortedByDescending { farmer -> farmer.soulEggs }
        PROPHECY_EGGS -> farmers.sortedByDescending { farmer -> farmer.prophecyEggs }
        PRESTIGES -> farmers.sortedByDescending { farmer -> farmer.prestiges }
        DRONE_TAKEDOWNS -> farmers.sortedByDescending { farmer -> farmer.droneTakedowns }
        ELITE_DRONE_TAKEDOWNS -> farmers.sortedByDescending { farmer -> farmer.eliteDroneTakedowns }
    }.let { sortedFarmers -> if (top != null) sortedFarmers.take(top) else sortedFarmers }
    val shortenedNames = sortedFarmers.map { farmer ->
        farmer.inGameName.let { name ->
            if (name.length <= 10) name
            else "${name.substring(0 until 9)}…"
        }
    }

    val boardTitle = when (board) {
        EARNINGS_BONUS -> "💵 Earnings Bonus"
        SOUL_EGGS -> "${context.emoteMention(Config.emoteSoulEgg) ?: "🥚"} Soul Eggs"
        PROPHECY_EGGS -> "${context.emoteMention(Config.emoteProphecyEgg) ?: "🥚"} Prophecy Eggs"
        PRESTIGES -> "🥨 Prestiges"
        DRONE_TAKEDOWNS -> "✈🚫 Drone Takedowns"
        ELITE_DRONE_TAKEDOWNS -> "🎖✈🚫 Elite Drone Takedowns"
    }

    title = "__**$boardTitle${if (!compact) " Leader Board" else ""}**__"
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
        alignment = Table.AlignedColumn.Alignment.RIGHT
        cells = when (board) {
            EARNINGS_BONUS -> sortedFarmers.map { farmer -> farmer.earningsBonus.formatIllions(shortened = true) + if (compact) "" else "${Typography.zwsp}%" }
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

