package nl.pindab0ter.eggbot.view

import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.BigDecimal.Companion.FOUR
import nl.pindab0ter.eggbot.helpers.BigDecimal.Companion.SIXTY
import nl.pindab0ter.eggbot.helpers.NumberFormatter.OPTIONAL_DECIMALS
import nl.pindab0ter.eggbot.model.simulation.SoloContractState
import org.joda.time.Duration
import java.math.BigDecimal


fun soloInfoResponse(
    state: SoloContractState,
    compact: Boolean = false,
): String = buildString message@{
    appendLine("`${state.farmer.name}` vs. _${state.contractName}_:")
    appendLine()

    drawGoals(state, compact)

    drawBasicInfo(state, compact)

    if (willReachBottleneckBeforeDone(state.farmer, state.timeRemaining, state.goals.last().moment))
        drawBottleNecks(state, compact)
}

fun soloFinishedIfCheckedInResponse(
    state: SoloContractState,
    compact: Boolean,
): String = buildString {
    appendLine("`${state.farmer.name}` vs. _${state.contractName}_:")
    appendLine()

    drawGoals(state, compact)

    drawFinishedBasicInfo(state, compact)
}

private fun StringBuilder.drawGoals(
    state: SoloContractState,
    compact: Boolean,
): StringBuilder = apply {
    val eggEmote = EggBot.eggsToEmotes[state.egg]?.asMention ?: "🥚"
    appendLine("__$eggEmote **Goals** (${state.goalsReached}/${state.goals.count()}):__ ```")
    state.goals.forEachIndexed { index, (goal, moment) ->
        append("${index + 1}. ")
        appendPaddingCharacters(
            goal.asIllions(OPTIONAL_DECIMALS),
            state.goals.map { it.target.asIllions(OPTIONAL_DECIMALS) }
        )

        append(goal.asIllions(OPTIONAL_DECIMALS))
        append(
            when {
                moment == null || moment > state.timeRemaining -> " 🔴 "
                moment == Duration.ZERO -> " 🏁 "
                else -> " 🟢 "
            }
        )

        when (moment) {
            null -> append("More than a year")
            Duration.ZERO -> append("Goal reached!")
            else -> append(moment.asDaysHoursAndMinutes(compact))
        }
        if (index + 1 < state.goals.count()) appendLine()
    }
    appendLine("```")
}

private fun StringBuilder.drawBasicInfo(
    state: SoloContractState,
    compact: Boolean,
): StringBuilder = apply {
    appendLine("__🗒️ **Basic info**:__ ```")
    appendLine("Time remaining:   ${state.timeRemaining.asDaysHoursAndMinutes(compact)}")

    append("Eggspected:       ${state.eggspected.asIllions()} ")
    if (!compact) append("(${
        minOf(
            eggIncrease(state.farmer.finalState.habs, state.farmer.constants),
            state.farmer.constants.transportRate
        ).multiply(SIXTY).asIllions()
    }/hr) ")
    appendLine()

    append("Current eggs:     ${state.farmer.initialState.eggsLaid.asIllions()} ")
    if (!compact) append("(${
        if (state.farmer.awayTimeRemaining <= Duration.ZERO) BigDecimal.ZERO.asIllions()
        else eggIncrease(state.farmer.initialState.habs, state.farmer.constants)
            .multiply(SIXTY).asIllions()
    }/hr) ")
    appendLine()

    append("Current chickens: ${state.farmer.initialState.population.asIllions()} ")
    if (!compact) append("(${
        chickenIncrease(state.farmer.initialState.habs, state.farmer.constants)
            .multiply(FOUR - state.farmer.initialState.habs.fullCount())
            .multiply(SIXTY).asIllions()
    }/hr)")
    appendLine()

    appendLine("Last update:      ${state.farmer.timeSinceBackup.asDaysHoursAndMinutes(compact)} ago")
    appendLine("```")
}

private fun StringBuilder.drawBottleNecks(
    state: SoloContractState,
    compact: Boolean,
): StringBuilder = apply {
    appendLine("__**⚠ Bottlenecks**__ ```")

    when (state.farmer.finalState.habsStatus) {
        is HabsStatus.BottleneckReached -> when (state.farmer.finalState.habsStatus.moment) {
            Duration.ZERO -> appendLine("🏠 Full! ")
            else -> appendLine("🏠 ${state.farmer.finalState.habsStatus.moment.asDaysHoursAndMinutes(compact)} ")
        }
        is HabsStatus.MaxedOut -> when (state.farmer.finalState.habsStatus.moment) {
            Duration.ZERO -> appendLine("🏠 Maxed! ")
            else -> appendLine("🏠 ${state.farmer.finalState.habsStatus.moment.asDaysHoursAndMinutes(compact)} ")
        }
        else -> Unit
    }

    when {
        state.farmer.finalState.transportBottleneck == null -> Unit
        state.farmer.finalState.transportBottleneck == Duration.ZERO ->
            appendLine("🚛 Full! ")
        state.farmer.finalState.transportBottleneck > Duration.ZERO ->
            appendLine("🚛 ${state.farmer.finalState.transportBottleneck.asDaysHoursAndMinutes(compact)} ")
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
    appendLine("__**🎉 Completed if you check in**:__ ```")
    appendLine("Time since backup: ${state.farmer.timeSinceBackup.asDaysHoursAndMinutes(compact)} ago")

    append("Current eggs:      ${state.farmer.initialState.eggsLaid.asIllions()} ")
    if (!compact) append("(${
        if (state.farmer.awayTimeRemaining <= Duration.ZERO) BigDecimal.ZERO.asIllions()
        else eggIncrease(state.farmer.initialState.habs, state.farmer.constants)
            .multiply(SIXTY).asIllions()
    }/hr) ")
    appendLine()

    append("Current chickens:  ${state.farmer.initialState.population.asIllions()} ")
    if (!compact) append("(${
        chickenIncrease(state.farmer.initialState.habs, state.farmer.constants)
            .multiply(FOUR - state.farmer.initialState.habs.fullCount())
            .multiply(SIXTY).asIllions()
    }/hr)")
    appendLine()

    appendLine("```")
}
