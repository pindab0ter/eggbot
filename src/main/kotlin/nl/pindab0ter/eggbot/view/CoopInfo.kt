package nl.pindab0ter.eggbot.view

import nl.pindab0ter.eggbot.EggBot.toEmote
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.BigDecimal.Companion.FOUR
import nl.pindab0ter.eggbot.helpers.BigDecimal.Companion.SIXTY
import nl.pindab0ter.eggbot.helpers.HabsStatus.BottleneckReached
import nl.pindab0ter.eggbot.helpers.HabsStatus.MaxedOut
import nl.pindab0ter.eggbot.helpers.NumberFormatter.OPTIONAL_DECIMALS
import nl.pindab0ter.eggbot.model.Table
import nl.pindab0ter.eggbot.model.Table.AlignedColumn.Alignment.RIGHT
import nl.pindab0ter.eggbot.model.simulation.CoopContractState
import nl.pindab0ter.eggbot.model.simulation.Farmer
import org.joda.time.Duration
import java.math.BigDecimal
import kotlin.random.Random


fun coopInfoResponse(
    state: CoopContractState,
    compact: Boolean = false,
): List<String> = buildString {
    val bottleneckedFarmers = state.farmers.zip(state.farmers.shortenedNames()).filter { (farmer, _) ->
        willReachBottleneckBeforeDone(farmer, state.timeRemaining, state.goals.last().moment)
    }

    appendLine("`${state.coopId}` vs. _${state.contractName}_:")

    drawGoals(state, compact)

    drawBasicInfo(state, compact = compact)

    append('\u200B')

    if (!compact) {
        drawMembers(state)
        if (bottleneckedFarmers.isNotEmpty()) {
            append('\u200B')
            drawBottleNecks(bottleneckedFarmers)
        }
    } else {
        drawCompactMembers(state)
        append('\u200B')
        drawCompactTokens(state)
        if (bottleneckedFarmers.isNotEmpty()) {
            append('\u200B')
            drawCompactBottleNecks(bottleneckedFarmers)
        }
    }
}.splitMessage(separator = '\u200B')

fun coopFinishedIfCheckedInResponse(
    state: CoopContractState,
    compact: Boolean,
): List<String> = buildString {
    appendLine("`${state.coopId}` vs. _${state.contractName}_:")

    drawGoals(state, compact)

    drawBasicInfo(state, finishedIfCheckedIn = true, compact)

    append('\u200B')

    if (!compact) {
        drawMembers(state)
        append('\u200B')
        drawTimeSinceLastBackup(state)
    } else {
        drawCompactMembers(state)
        append('\u200B')
        drawCompactTimeSinceLastBackup(state)
    }
}.splitMessage(separator = '\u200B')

private fun List<Farmer>.shortenedNames(): List<String> = map { farmer ->
    farmer.name.let { name ->
        if (name.length <= 10) name
        else "${name.substring(0 until 9)}…"
    }
}

private fun StringBuilder.drawGoals(
    coopContractState: CoopContractState,
    compact: Boolean,
): StringBuilder = appendTable {
    title = "__${coopContractState.egg.toEmote()} **Goals** (${coopContractState.goalsReached}/${coopContractState.goals.count()}):__"
    displayHeader = false
    topPadding = 1

    incrementColumn(suffix = ".")
    column {
        leftPadding = 1
        cells = coopContractState.goals.map { (target, _) -> target.asIllions(OPTIONAL_DECIMALS) }
    }
    column {
        leftPadding = 2
        rightPadding = 2
        cells = coopContractState.goals.map { (_, moment) ->
            when {
                moment == null || moment > coopContractState.timeRemaining -> "🔴"
                moment == Duration.ZERO -> "🏁"
                else -> "🟢"
            }
        }
    }
    column {
        cells = coopContractState.goals.map { (_, moment) ->
            when (moment) {
                null -> "More than a year"
                Duration.ZERO -> "Goal reached!"
                else -> moment.asDaysHoursAndMinutes(compact)
            }
        }
    }
}

private fun StringBuilder.drawBasicInfo(
    coopContractState: CoopContractState,
    finishedIfCheckedIn: Boolean = false,
    compact: Boolean,
): StringBuilder = apply {

    appendLine()

    appendLine("__🗒️ **Basic info**:__ ```")

    if (!finishedIfCheckedIn) {
        appendLine("Time remaining:   ${coopContractState.timeRemaining.asDaysHoursAndMinutes(compact)}")
        append("Eggspected:       ${coopContractState.eggspected.asIllions()} ")
        appendLine()
    }

    append("Current eggs:     ${
        coopContractState.farmers.sumByBigDecimal { farmer -> farmer.initialState.eggsLaid }.asIllions()
    } ")
    if (!compact) append("(${
        coopContractState.farmers.sumByBigDecimal { farmer ->
            eggIncrease(farmer.initialState.habs, farmer.constants)
        }.multiply(SIXTY).asIllions()
    }/hr)")
    appendLine()

    append("Current chickens: ${
        coopContractState.farmers.sumByBigDecimal { farmer -> farmer.initialState.population }.asIllions()
    } ")
    if (!compact) append("(${
        coopContractState.farmers.sumByBigDecimal { farmer ->
            chickenIncrease(farmer.initialState.habs, farmer.constants)
                .multiply(FOUR - farmer.initialState.habs.fullCount())
        }.multiply(SIXTY).asIllions()
    }/hr)")
    appendLine()

    appendLine("Tokens available: ${coopContractState.tokensAvailable}")
    appendLine("Tokens spent:     ${coopContractState.tokensSpent}")
    if (coopContractState.public) appendLine("Access:           This co-op is PUBLIC")
    appendLine("```\u200B")
}

private fun StringBuilder.drawMembers(
    state: CoopContractState,
): StringBuilder = appendTable {
    val memberEmoji = when (Random.nextBoolean()) {
        true -> "👨‍🌾"
        false -> "👩‍🌾"
    }
    title = "__**${memberEmoji} Members** (${state.farmers.count()}/${state.maxCoopSize}):__"

    incrementColumn(":")

    column {
        header = "Name"
        leftPadding = 1
        rightPadding = 3
        cells = state.farmers.map { farmer -> "${farmer.name}${if (farmer.isSleeping) " zZ" else ""}" }
    }

    column {
        header = "Eggs"
        alignment = RIGHT
        cells = state.farmers.map { farmer -> farmer.initialState.eggsLaid.asIllions() }
    }

    overtakersColumn(state) {
        leftPadding = 1
    }

    divider()

    column {
        header = "/hr"
        rightPadding = 3
        cells = state.farmers.map { farmer ->
            if (farmer.awayTimeRemaining <= Duration.ZERO) BigDecimal.ZERO.asIllions()
            else eggIncrease(farmer.initialState.habs, farmer.constants).multiply(SIXTY).asIllions()
        }
    }
    column {
        header = "Chickens"
        alignment = RIGHT
        cells = state.farmers.map { farmer -> farmer.initialState.population.asIllions() }
    }

    divider()

    column {
        header = "/hr"
        rightPadding = 3
        cells = state.farmers.map { farmer ->
            chickenIncrease(farmer.initialState.habs, farmer.constants)
                .multiply(FOUR - farmer.initialState.habs.fullCount())
                .multiply(SIXTY).asIllions()
        }
    }

    column {
        header = "Tkns"
        alignment = RIGHT
        cells = state.farmers.map { farmer ->
            if (farmer.constants.tokensAvailable > 0) "${farmer.constants.tokensAvailable}" else ""
        }
    }

    divider()

    column {
        header = "Spent"
        cells = state.farmers.map { farmer ->
            if (farmer.constants.tokensSpent > 0) "${farmer.constants.tokensSpent}" else ""
        }
    }
}

private fun StringBuilder.drawCompactMembers(
    state: CoopContractState,
): StringBuilder = appendTable {
    val memberEmoji = when (Random.nextBoolean()) {
        true -> "👨‍🌾"
        false -> "👩‍🌾"
    }
    title = "__**${memberEmoji} Members** (${state.farmers.count()}/${state.maxCoopSize}):__"
    topPadding = 1

    column {
        header = "Name"
        rightPadding = 1
        cells = state.farmers.zip(state.farmers.shortenedNames()).map { (farmer, name) ->
            "$name${if (farmer.isSleeping) " zZ" else ""}"
        }
    }

    overtakersColumn(state) {
        rightPadding = 2
    }

    column {
        header = "Eggs"
        alignment = RIGHT
        cells = state.farmers.map { farmer -> farmer.initialState.eggsLaid.asIllions() }
    }

    divider()

    column {
        header = "/hr"
        rightPadding = 2
        cells = state.farmers.map { farmer ->
            eggIncrease(farmer.initialState.habs, farmer.constants).multiply(SIXTY).asIllions()
        }
    }
}

private fun Table.overtakersColumn(state: CoopContractState, init: Table.EmojiColumn.() -> Unit) {
    val overtakers: List<String> = state.farmers.map { farmer ->
        when {
            state.farmers.any { other ->
                eggIncrease(farmer.finalState.habs, farmer.constants) >
                        eggIncrease(other.finalState.habs, other.constants) &&
                        farmer.finalState.eggsLaid < other.finalState.eggsLaid
            } -> "⬆️"
            state.farmers.any { other ->
                eggIncrease(farmer.finalState.habs, farmer.constants) <
                        eggIncrease(other.finalState.habs, other.constants) &&
                        farmer.finalState.eggsLaid > other.finalState.eggsLaid
            } -> "⬇️"
            else -> "➖"
        }
    }

    if (overtakers.any { it != "➖" }) emojiColumn {
        header = if (Random.nextBoolean()) "🏃‍♂️" else "🏃‍♀️"
        cells = overtakers
        init()
    }
}

private fun StringBuilder.drawBottleNecks(
    bottleneckedFarmers: List<Pair<Farmer, String>>,
): StringBuilder = appendTable {
    title = "__**⚠ Bottlenecks**__"
    topPadding = 1

    column {
        header = "Name"
        rightPadding = 2
        cells = bottleneckedFarmers.map { (farmer, _) -> farmer.name }
    }

    column {
        header = "Habs"
        leftPadding = 1
        alignment = RIGHT
        cells = bottleneckedFarmers.map { (farmer, _) ->
            when (farmer.finalState.habsStatus) {
                is BottleneckReached -> when (farmer.finalState.habsStatus.moment) {
                    Duration.ZERO -> "Full!"
                    else -> farmer.finalState.habsStatus.moment.asDaysHoursAndMinutes(true)
                }
                is MaxedOut -> when (farmer.finalState.habsStatus.moment) {
                    Duration.ZERO -> "Maxed!"
                    else -> farmer.finalState.habsStatus.moment.asDaysHoursAndMinutes(true)
                }
                else -> ""
            }
        }
    }

    emojiColumn {
        header = "🏘️"
        leftPadding = 1
        cells = bottleneckedFarmers.map { (farmer, _) ->
            when (farmer.finalState.habsStatus) {
                is BottleneckReached -> if (farmer.finalState.habsStatus.moment == Duration.ZERO) "🛑" else "⚠️"
                is MaxedOut -> "🟢"
                else -> "➖"
            }
        }
    }

    divider()

    column {
        header = "Transport"
        leftPadding = 1
        alignment = RIGHT
        cells = bottleneckedFarmers.map { (farmer, _) ->
            when (farmer.finalState.transportBottleneck) {
                null -> ""
                Duration.ZERO -> "Full!"
                else -> farmer.finalState.transportBottleneck.asDaysHoursAndMinutes(true)
            }
        }
    }

    emojiColumn {
        header = "🚛"
        leftPadding = 1
        cells = bottleneckedFarmers.map { (farmer, _) ->
            when (farmer.finalState.transportBottleneck) {
                null -> "➖"
                Duration.ZERO -> "🛑"
                else -> "⚠️"
            }
        }
    }

    divider()

    column {
        header = "Silos"
        leftPadding = 1
        alignment = RIGHT
        cells = bottleneckedFarmers.map { (farmer, _) ->
            when {
                farmer.awayTimeRemaining <= Duration.ZERO -> "Empty!"
                farmer.awayTimeRemaining < Duration.standardHours(12L) ->
                    farmer.awayTimeRemaining.asDaysHoursAndMinutes(true)
                else -> ""
            }
        }
    }

    emojiColumn {
        header = "⌛"
        leftPadding = 1
        cells = bottleneckedFarmers.map { (farmer, _) ->
            when {
                farmer.awayTimeRemaining <= Duration.ZERO ->
                    "🛑"
                farmer.awayTimeRemaining < Duration.standardHours(12L) ->
                    "⚠️"
                else -> "➖"
            }
        }
    }

    divider(intersection = '╡')
}

private fun StringBuilder.drawCompactBottleNecks(
    bottleneckedFarmers: List<Pair<Farmer, String>>,
): StringBuilder = appendTable {
    title = "__**⚠ Bottlenecks**__"
    topPadding = 1

    column {
        header = "Name"
        cells = bottleneckedFarmers.map { (_, shortenedName) -> shortenedName }
    }

    emojiColumn {
        header = "🏘️"
        leftPadding = 1
        cells = bottleneckedFarmers.map { (farmer, _) ->
            when (farmer.finalState.habsStatus) {
                is BottleneckReached -> if (farmer.finalState.habsStatus.moment == Duration.ZERO) "🛑" else "⚠️"
                is MaxedOut -> "🟢"
                else -> "➖"
            }
        }
    }

    divider()

    emojiColumn {
        header = "🚛"
        cells = bottleneckedFarmers.map { (farmer, _) ->
            when (farmer.finalState.transportBottleneck) {
                null -> "➖"
                Duration.ZERO -> "🛑"
                else -> "⚠️"
            }
        }
    }

    divider()

    emojiColumn {
        header = "⌛"
        cells = bottleneckedFarmers.map { (farmer, _) ->
            when {
                farmer.awayTimeRemaining <= Duration.ZERO ->
                    "🛑"
                farmer.awayTimeRemaining < Duration.standardHours(12L) ->
                    "⚠️"
                else -> "➖"
            }
        }
    }
}

private fun StringBuilder.drawCompactTokens(
    coopContractState: CoopContractState,
): StringBuilder = appendTable {
    title = "__**🎫 Tokens**__"
    topPadding = 1

    column {
        header = "Name"
        rightPadding = 2
        cells = coopContractState.farmers.shortenedNames()
    }
    column {
        header = "Tokens"
        alignment = RIGHT
        cells = coopContractState.farmers.map { farmer -> "${farmer.constants.tokensAvailable}" }
    }
    divider()
    column {
        header = "Spent"
        cells = coopContractState.farmers.map { farmer -> "${farmer.constants.tokensSpent}" }
    }
}

private fun StringBuilder.drawTimeSinceLastBackup(
    state: CoopContractState,
): StringBuilder = appendTable {
    title = "__**🎉 Completed if everyone checks in**:__"
    topPadding = 1

    val farmersSortedByTimeSinceBackup = state.farmers.sortedByDescending { farmer -> farmer.timeSinceBackup }

    column {
        header = "Name"
        leftPadding = 1
        rightPadding = 3
        cells = farmersSortedByTimeSinceBackup.map { farmer ->
            farmer.name
        }
    }

    divider()

    column {
        header = "Last update"
        leftPadding = 1
        cells = farmersSortedByTimeSinceBackup.map { farmer -> "${farmer.timeSinceBackup.asDaysHoursAndMinutes()} ago" }
    }
}

private fun StringBuilder.drawCompactTimeSinceLastBackup(
    state: CoopContractState,
): StringBuilder = appendTable {
    title = "__**🎉 Completed if everyone checks in**:__"
    topPadding = 1

    val sortedFarmers = state.farmers.sortedByDescending { farmer -> farmer.timeSinceBackup }

    column {
        header = "Name"
        rightPadding = 1
        cells = sortedFarmers.shortenedNames()
    }

    column {
        header = "Last update"
        leftPadding = 1
        alignment = RIGHT
        cells = sortedFarmers.map { farmer -> "${farmer.timeSinceBackup.asDaysHoursAndMinutes(true)} ago" }
    }
}
