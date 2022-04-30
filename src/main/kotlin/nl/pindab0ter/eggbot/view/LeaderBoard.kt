package nl.pindab0ter.eggbot.view

import com.kotlindiscord.kord.extensions.commands.CommandContext
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.DisplayMode.COMPACT
import nl.pindab0ter.eggbot.helpers.DisplayMode.EXTENDED
import nl.pindab0ter.eggbot.helpers.Typography.zwsp
import nl.pindab0ter.eggbot.model.Config
import nl.pindab0ter.eggbot.model.LeaderBoard
import nl.pindab0ter.eggbot.model.LeaderBoard.*
import nl.pindab0ter.eggbot.model.Table
import nl.pindab0ter.eggbot.model.database.Farmer

@KordPreview
suspend fun leaderboardResponse(
    farmers: List<Farmer>,
    leaderBoard: LeaderBoard,
    top: Int?,
    displayMode: DisplayMode?,
    context: CommandContext,
): List<String> = table {

    val compact = displayMode == COMPACT

    val sortedFarmers = when (leaderBoard) {
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

    val boardTitle = when (leaderBoard) {
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
        header = when (leaderBoard) {
            EARNINGS_BONUS -> "Earnings Bonus" + if (compact) "" else "  " // Added spacing for percent suffix
            SOUL_EGGS -> "Soul Eggs"
            PROPHECY_EGGS -> "Prophecy Eggs"
            PRESTIGES -> "Prestiges"
            DRONE_TAKEDOWNS -> "Drone Takedowns"
            ELITE_DRONE_TAKEDOWNS -> "Elite Drone Takedowns"
        }

        alignment = Table.AlignedColumn.Alignment.RIGHT

        cells = when (leaderBoard) {
            EARNINGS_BONUS -> sortedFarmers.map { farmer ->
                when (displayMode) {
                    EXTENDED -> "${farmer.earningsBonus.formatInteger()}$zwsp%"
                    else -> "${farmer.earningsBonus.formatIllions(shortened = compact)}${if (compact) "" else "$zwsp%"}"
                }
            }
            SOUL_EGGS -> sortedFarmers.map { farmer ->
                when (displayMode) {
                    EXTENDED -> farmer.soulEggs.formatInteger()
                    else -> farmer.soulEggs.formatIllions(shortened = compact)
                }
            }
            PROPHECY_EGGS -> sortedFarmers.map { farmer -> farmer.prophecyEggs.formatInteger() }
            PRESTIGES -> sortedFarmers.map { farmer -> farmer.prestiges.formatInteger() }
            DRONE_TAKEDOWNS -> sortedFarmers.map { farmer -> farmer.droneTakedowns.formatInteger() }
            ELITE_DRONE_TAKEDOWNS -> sortedFarmers.map { farmer -> farmer.eliteDroneTakedowns.formatInteger() }
        }
    }

    if (leaderBoard == EARNINGS_BONUS) column {
        header = if (compact) "Role" else "Farmer Role"
        leftPadding = if (compact) 1 else 2
        cells = sortedFarmers.map { farmer -> farmer.earningsBonus.formatRank(shortened = compact) }
    }
}

