package nl.pindab0ter.eggbot.view

import dev.kord.core.behavior.GuildBehavior
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.Server
import nl.pindab0ter.eggbot.ZERO_WIDTH_SPACE
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.DisplayMode.*
import nl.pindab0ter.eggbot.model.LeaderBoard
import nl.pindab0ter.eggbot.model.LeaderBoard.*
import nl.pindab0ter.eggbot.model.Table.AlignedColumn.Alignment.RIGHT
import nl.pindab0ter.eggbot.model.database.Farmer

fun GuildBehavior.leaderboardResponse(
    farmers: List<Farmer>,
    leaderBoard: LeaderBoard,
    top: Int? = null,
    displayMode: DisplayMode = REGULAR,
    server: Server,
): List<String> = table {
    val compact = displayMode == COMPACT

    val sortedFarmers = when (leaderBoard) {
        EARNINGS_BONUS -> farmers.sortedByDescending { farmer -> farmer.earningsBonus }
        SOUL_EGGS -> farmers.sortedByDescending { farmer -> farmer.soulEggs }
        PROPHECY_EGGS -> farmers.sortedByDescending { farmer -> farmer.prophecyEggs }
        PRESTIGES -> farmers.sortedByDescending { farmer -> farmer.prestiges }
        DRONE_TAKEDOWNS -> farmers.sortedByDescending { farmer -> farmer.droneTakedowns }
        ELITE_DRONE_TAKEDOWNS -> farmers.sortedByDescending { farmer -> farmer.eliteDroneTakedowns }
        ROCKET_LAUNCHES -> farmers.sortedByDescending { farmer -> farmer.rocketsLaunched }
    }.let { sortedFarmers -> if (top != null) sortedFarmers.take(top) else sortedFarmers }

    val shortenedNames: List<String> = sortedFarmers.map { farmer ->
        when {
            farmer.inGameName.length <= 10 -> farmer.inGameName
            else -> "${farmer.inGameName.substring(0 until 9)}…"
        }
    }

    val soulEgg = runBlocking { getEmojiOrNull(server.emote.soulEgg)?.mention } ?: "🥚"
    val prophecyEgg = runBlocking { getEmojiOrNull(server.emote.prophecyEgg)?.mention } ?: "🥚"

    val boardTitle = when (leaderBoard) {
        EARNINGS_BONUS -> "💵 Earnings Bonus"
        SOUL_EGGS -> "$soulEgg Soul Eggs"
        PROPHECY_EGGS -> "$prophecyEgg Prophecy Eggs"
        PRESTIGES -> "🥨 Prestiges"
        DRONE_TAKEDOWNS -> "✈🚫 Drone Takedowns"
        ELITE_DRONE_TAKEDOWNS -> "🎖✈🚫 Elite Drone Takedowns"
        ROCKET_LAUNCHES -> "🚀 Rockets Launched"
    }

    title = "__**$boardTitle${if (!compact) " Leader Board" else ""}**__"
    displayHeaders = true
    if (compact) incrementColumn() else incrementColumn(":")
    column {
        header = "Name"
        leftPadding = 1
        rightPadding = if (compact) 1 else 2
        cells = when {
            compact -> shortenedNames
            else -> sortedFarmers.map(Farmer::inGameName)
        }
    }

    column {
        header = when (leaderBoard) {
            EARNINGS_BONUS -> "Earnings Bonus" + if (compact) "" else "  " // Added spacing for percent suffix
            SOUL_EGGS -> "Soul Eggs"
            PROPHECY_EGGS -> "Prophecy Eggs"
            PRESTIGES -> "Prestiges"
            DRONE_TAKEDOWNS -> "Drone Takedowns"
            ELITE_DRONE_TAKEDOWNS -> "Elite Drone Takedowns"
            ROCKET_LAUNCHES -> "Rockets Launched"
        }

        alignment = RIGHT

        cells = when (leaderBoard) {
            EARNINGS_BONUS -> sortedFarmers.map { farmer ->
                when (displayMode) {
                    EXTENDED -> "${farmer.earningsBonus.formatInteger()}$ZERO_WIDTH_SPACE%"
                    else -> "${farmer.earningsBonus.formatIllions(shortened = compact)}${if (compact) "" else "$ZERO_WIDTH_SPACE%"}"
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
            ROCKET_LAUNCHES -> sortedFarmers.map { farmer -> farmer.rocketsLaunched.formatInteger() }
        }
    }

    if (leaderBoard == EARNINGS_BONUS) column {
        header = if (compact) "Role" else "Farmer Role"
        leftPadding = if (compact) 1 else 2
        cells = sortedFarmers.map { farmer -> farmer.earningsBonus.formatRank(shortened = compact) }
    }
}
