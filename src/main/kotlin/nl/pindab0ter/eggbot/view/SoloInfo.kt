package nl.pindab0ter.eggbot.view

import dev.kord.core.entity.Guild
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.BigDecimal.Companion.SIXTY
import nl.pindab0ter.eggbot.helpers.NumberFormatter.OPTIONAL_DECIMALS
import nl.pindab0ter.eggbot.model.Table
import nl.pindab0ter.eggbot.model.simulation.SoloContractState
import org.joda.time.Duration


fun Guild.soloInfoResponse(
    state: SoloContractState,
    compact: Boolean = false,
): String = buildString message@{
    appendLine("`${state.farmer.inGameName}` vs. _${state.contractName}_:")

    drawGoals(state, compact, this@soloInfoResponse)

    drawBasicInfo(state, compact)

    if (willReachBottleneckBeforeDone(state.farmer, state.timeRemaining, state.goals.last().moment))
        drawBottleNecks(state, compact)
}

fun Guild.soloFinishedIfBankedResponse(
    state: SoloContractState,
    compact: Boolean,
): String = buildString {
    appendLine("`${state.farmer.inGameName}` vs. _${state.contractName}_:")
    appendLine()

    drawGoals(state, compact, this@soloFinishedIfBankedResponse)

    drawFinishedBasicInfo(state, compact)
}

private fun StringBuilder.drawGoals(
    state: SoloContractState,
    compact: Boolean,
    discordGuild: Guild?
): StringBuilder = appendTable {
    val eggEmoteMention = discordGuild?.emoteMention(state.egg)

    title = "__${eggEmoteMention} **Goals** (${state.goalsReached}/${state.goals.count()})__"
    displayHeaders = false
    topPadding = 1

    incrementColumn(suffix = ".")
    column {
        leftPadding = 1
        cells = state.goals.map { (target, _) -> target.formatIllions(OPTIONAL_DECIMALS) }
    }
    column {
        leftPadding = 1
        rightPadding = 1
        cells = state.goals.map { (_, moment) ->
            when {
                moment == null || moment > state.timeRemaining -> "🔴"
                moment == Duration.ZERO -> "🏁"
                else -> "🟢"
            }
        }
    }
    column {
        cells = state.goals.map { (_, moment) ->
            when (moment) {
                null -> "More than a year"
                Duration.ZERO -> "Goal reached!"
                else -> moment.formatDaysHoursAndMinutes(compact)
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

        cells = buildList {
            if (!state.finishedIfBanked) {
                add("Time remaining:")
                add("Eggspected:")
            }
            add("Current eggs:")
            add("Banked eggs:")
            add("Unbanked eggs:")
            add("Current chickens:")
            add("Tokens available:")
            add("Tokens spent:")
            add("Last update:")
        }
    }

    column {
        alignment = if (!compact) Table.AlignedColumn.Alignment.LEFT else Table.AlignedColumn.Alignment.RIGHT

        cells = buildList {
            if (!state.finishedIfBanked) {
                add(state.timeRemaining.formatDaysHoursAndMinutes(compact, compact))
                add(state.timeUpEggsLaid.formatIllions())
            }
            add(
                state.farmer.currentEggsLaid.formatIllions() + if (!compact)
                    " (${state.farmer.currentEggsPerMinute.multiply(SIXTY).formatIllions()}/hr)" else ""
            )
            add(state.reportedEggsLaid.formatIllions())
            add(state.farmer.unreportedEggsLaid.formatIllions())
            add(
                state.reportedPopulation.formatIllions() + if (!compact)
                    " (${state.reportedPopulationIncreasePerMinute.multiply(SIXTY).formatIllions()}/hr)" else ""
            )
            add(state.farmer.constants.tokensAvailable.toString())
            add(state.farmer.constants.tokensSpent.toString())
            add("${state.farmer.timeSinceBackup.formatDaysHoursAndMinutes(compact)} ago")
        }
    }
}

private fun StringBuilder.drawBottleNecks(
    state: SoloContractState,
    compact: Boolean,
): StringBuilder = apply {
    appendLine()
    appendLine("__**⚠ Bottlenecks**__ ```")

    when (state.farmer.runningState.habsStatus) {
        is HabsStatus.BottleneckReached -> {
            val moment = state.farmer.runningState.habsStatus.moment
            when {
                moment == Duration.ZERO -> appendLine("🏠 Full! ")
                moment < state.timeRemaining && moment < (state.timeTillFinalGoal ?: ONE_YEAR) ->
                    appendLine("🏠 ${state.farmer.runningState.habsStatus.moment.formatDaysHoursAndMinutes(compact)} ")
                else -> Unit
            }
        }
        is HabsStatus.MaxedOut -> when (state.farmer.runningState.habsStatus.moment) {
            Duration.ZERO -> appendLine("🏠 Maxed! ")
            else -> appendLine("🏠 ${state.farmer.runningState.habsStatus.moment.formatDaysHoursAndMinutes(compact)} ")
        }
        else -> Unit
    }

    val transportBottleneckMoment = state.farmer.runningState.transportBottleneck
    when {
        transportBottleneckMoment == null -> Unit
        transportBottleneckMoment == Duration.ZERO -> appendLine("🚛 Full! ")
        transportBottleneckMoment < state.timeRemaining && transportBottleneckMoment < (state.timeTillFinalGoal ?: ONE_YEAR) ->
            appendLine("🚛 ${transportBottleneckMoment.formatDaysHoursAndMinutes(compact)} ")
        else -> Unit
    }

    when {
        state.farmer.awayTimeRemaining <= Duration.ZERO -> appendLine("⌛ Empty!")
        state.farmer.awayTimeRemaining < Duration.standardHours(12L)
                && state.farmer.awayTimeRemaining < state.timeRemaining
                && state.farmer.awayTimeRemaining < (state.timeTillFinalGoal ?: ONE_YEAR) ->
            appendLine("⌛ ${state.farmer.awayTimeRemaining.formatDaysHoursAndMinutes(compact)}")
        else -> Unit
    }
    appendLine("```")
}

private fun StringBuilder.drawFinishedBasicInfo(
    state: SoloContractState,
    compact: Boolean,
): StringBuilder = apply {
    appendLine("__**🎉 Bank now to finish!**__ ```")
    appendLine("Time since backup: ${state.farmer.timeSinceBackup.formatDaysHoursAndMinutes(compact)} ago")

    append("Current eggs:   ${state.farmer.currentEggsLaid.formatIllions()} ")
    if (!compact) append("(${state.farmer.currentEggsPerMinute.multiply(SIXTY).formatIllions()}/hr) ")
    appendLine()

    append("Unbanked eggs:  ${state.farmer.unreportedEggsLaid.formatIllions()} ")
    appendLine()

    appendLine("```")
}
