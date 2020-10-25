package nl.pindab0ter.eggbot.view

import nl.pindab0ter.eggbot.EggBot.toEmote
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.BigDecimal.Companion.SIXTY
import nl.pindab0ter.eggbot.helpers.NumberFormatter.OPTIONAL_DECIMALS
import nl.pindab0ter.eggbot.model.Table
import nl.pindab0ter.eggbot.model.simulation.SoloContractState
import org.joda.time.Duration


fun soloInfoResponse(
    state: SoloContractState,
    compact: Boolean = false,
): String = buildString message@{
    appendLine("`${state.farmer.name}` vs. _${state.contractName}_:")

    drawGoals(state, compact)

    drawBasicInfo(state, compact)

    if (willReachBottleneckBeforeDone(state.farmer, state.timeRemaining, state.goals.last().moment))
        drawBottleNecks(state, compact)
}

fun soloFinishedIfBankedResponse(
    state: SoloContractState,
    compact: Boolean,
): String = buildString {
    appendLine("`${state.farmer.name}` vs. _${state.contractName}_:")
    appendLine()

    drawGoals(state, compact)

    drawFinishedBasicInfo(state, compact)
}

private fun StringBuilder.drawGoals(
    coopContractState: SoloContractState,
    compact: Boolean,
): StringBuilder = appendTable {
    title = "__${coopContractState.egg.toEmote()} **Goals** (${coopContractState.goalsReached}/${coopContractState.goals.count()}):__"
    displayHeaders = false
    topPadding = 1

    incrementColumn(suffix = ".")
    column {
        leftPadding = 1
        cells = coopContractState.goals.map { (target, _) -> target.asIllions(OPTIONAL_DECIMALS) }
    }
    column {
        leftPadding = 1
        rightPadding = 1
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
    state: SoloContractState,
    compact: Boolean,
): StringBuilder = appendTable {
    title = "__**🗒️ Basic info**__"
    displayHeaders = false
    topPadding = 1

    column {
        alignment = Table.AlignedColumn.Alignment.LEFT
        rightPadding = 1

        val progressKeys = if (state.finishedIfBanked) emptyList() else listOf(
            "Time remaining:",
            "Eggspected:",
        )
        val statusKeys = listOf(
            "Current eggs:",
            "Banked eggs:",
            "Current chickens:",
            "Tokens available:",
            "Tokens spent:",
            "Last update:",
        )

        cells = progressKeys + statusKeys
    }

    column {
        alignment = if (!compact) Table.AlignedColumn.Alignment.LEFT else Table.AlignedColumn.Alignment.RIGHT

        val progressValues = if (state.finishedIfBanked) emptyList() else listOf(
            state.timeRemaining.asDaysHoursAndMinutes(compact, compact),
            state.timeUpEggsLaid.asIllions(),
        )
        val statusValues = listOf(
            state.farmer.reportedEggsLaid.asIllions() + if (!compact)
                "(${state.reportedEggsPerMinute.multiply(SIXTY).asIllions()}/hr)" else "",
            state.reportedEggsLaid.asIllions(),
            state.reportedPopulation.asIllions() + if (!compact)
                "(${state.reportedPopulationIncreasePerMinute.multiply(SIXTY).asIllions()}/hr)" else "",
            state.farmer.constants.tokensAvailable.toString(),
            state.farmer.constants.tokensSpent.toString(),
            "${state.farmer.timeSinceBackup.asDaysHoursAndMinutes(compact)} ago",
        )

        cells = progressValues + statusValues
    }
}

private fun StringBuilder.drawBottleNecks(
    state: SoloContractState,
    compact: Boolean,
): StringBuilder = apply {
    appendLine("__**⚠ Bottlenecks**__ ```")

    when (state.farmer.runningState.habsStatus) {
        is HabsStatus.BottleneckReached -> when (state.farmer.runningState.habsStatus.moment) {
            Duration.ZERO -> appendLine("🏠 Full! ")
            else -> appendLine("🏠 ${state.farmer.runningState.habsStatus.moment.asDaysHoursAndMinutes(compact)} ")
        }
        is HabsStatus.MaxedOut -> when (state.farmer.runningState.habsStatus.moment) {
            Duration.ZERO -> appendLine("🏠 Maxed! ")
            else -> appendLine("🏠 ${state.farmer.runningState.habsStatus.moment.asDaysHoursAndMinutes(compact)} ")
        }
        else -> Unit
    }

    when {
        state.farmer.runningState.transportBottleneck == null -> Unit
        state.farmer.runningState.transportBottleneck == Duration.ZERO ->
            appendLine("🚛 Full! ")
        state.farmer.runningState.transportBottleneck > Duration.ZERO ->
            appendLine("🚛 ${state.farmer.runningState.transportBottleneck.asDaysHoursAndMinutes(compact)} ")
    }

    when {
        state.farmer.awayTimeRemaining < Duration.ZERO ->
            appendLine("⌛ Empty!")
        state.farmer.awayTimeRemaining < Duration.standardHours(12L) ->
            appendLine("⌛ ${state.farmer.awayTimeRemaining.asDaysHoursAndMinutes(compact)}")
    }
    appendLine("```")
}

private fun StringBuilder.drawFinishedBasicInfo(
    state: SoloContractState,
    compact: Boolean,
): StringBuilder = apply {
    appendLine("__**🎉 Completed if you check in**__ ```")
    appendLine("Time since backup: ${state.farmer.timeSinceBackup.asDaysHoursAndMinutes(compact)} ago")

    append("Current eggs:      ${state.farmer.currentEggsLaid.asIllions()} ")
    if (!compact) append("(${state.farmer.currentEggsPerMinute.multiply(SIXTY).asIllions()}/hr) ")
    appendLine()

    append("Current chickens:  ${state.farmer.currentChickens.asIllions()} ")
    if (!compact) append("(${state.farmer.currentChickenIncreasePerMinute.multiply(SIXTY).asIllions()}/hr)")
    appendLine()

    appendLine("```")
}
