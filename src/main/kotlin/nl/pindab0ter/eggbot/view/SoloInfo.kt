package nl.pindab0ter.eggbot.view

import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.model.Config
import nl.pindab0ter.eggbot.model.simulation.new.SoloContractState
import org.joda.time.Duration
import java.math.BigDecimal


fun soloInfoResponse(
    contract: SoloContractState,
    compact: Boolean = false,
): String = StringBuilder().apply message@{
    val eggEmote = EggBot.eggsToEmotes[contract.egg]?.asMention ?: "🥚"

    appendLine("`${contract.farmer.name}` vs. _${contract.contractName}_:")
    appendLine()

    if (contract.finished) {
        appendLine("**You have successfully finished this contract! ${Config.emojiSuccess}**")
        return@message
    }

    // region Goals

    appendLine("__$eggEmote **Goals** (${contract.goalsReached}/${contract.goals.count()}):__ ```")
    contract.goals.forEachIndexed { index, (goal, moment) ->
        append("${index + 1}. ")
        appendPaddingCharacters(
            goal.asIllions(NumberFormatter.OPTIONAL_DECIMALS),
            contract.goals.map { it.target.asIllions(NumberFormatter.OPTIONAL_DECIMALS) }
        )
        append(goal.asIllions(NumberFormatter.OPTIONAL_DECIMALS))
        append(
            when (moment) {
                null -> " 🔴 "
                Duration.ZERO -> " 🏁 "
                else -> " 🟢 "
            }
        )
        when (moment) {
            null -> append("More than a year")
            Duration.ZERO -> append("Goal reached!")
            else -> append(moment.asDaysHoursAndMinutes(compact))
        }
        if (index + 1 < contract.goals.count()) appendLine()
    }
    appendLine("```")

    // endregion Goals

    // region Basic info and totals

    appendLine("__🗒️ **Basic info**:__ ```")
    append("Eggspected:       ${contract.eggspected.asIllions()} ")
    if (!compact) append("(${
        minOf(
            eggIncrease(contract.farmer.finalState.habs, contract.farmer.constants),
            contract.farmer.constants.transportRate
        ).multiply(BigDecimal(60L)).asIllions()
    }/hr) ")
    appendLine()
    appendLine("Time remaining:   ${contract.timeRemaining.asDaysHoursAndMinutes(compact)}")
    append("Current chickens: ${contract.farmer.initialState.population.asIllions()} ")
    if (!compact) append("(${
        chickenIncrease(contract.farmer.initialState.habs, contract.farmer.constants)
            .multiply(BigDecimal(4L) - contract.farmer.initialState.habs.fullCount())
            .multiply(BigDecimal(60L)).asIllions()
    }/hr)")
    appendLine()
    append("Current eggs:     ${contract.farmer.initialState.eggsLaid.asIllions()} ")
    if (!compact) append("(${
        eggIncrease(contract.farmer.initialState.habs, contract.farmer.constants).multiply(BigDecimal(60L)).asIllions()
    }/hr) ")
    appendLine()
    appendLine("Last update:      ${contract.farmer.timeSinceBackup.asDaysHoursAndMinutes(compact)} ago")
    appendLine("```")

    // endregion Basic info and totals

    // region Bottlenecks

    contract.apply {
        if (willReachBottlenecks(farmer, contract.goals.last().moment)) {
            this@message.appendLine("__**⚠ Bottlenecks**__ ```")
            when {
                farmer.finalState.habBottleneck == null -> Unit
                farmer.finalState.habBottleneck == Duration.ZERO ->
                    appendLine("🏠 Full! ")
                farmer.finalState.habBottleneck > Duration.ZERO ->
                    appendLine("🏠 ${farmer.finalState.habBottleneck.asDaysHoursAndMinutes(compact)} ")
            }
            when {
                farmer.finalState.transportBottleneck == null -> Unit
                farmer.finalState.transportBottleneck == Duration.ZERO ->
                    appendLine("🚛 Full! ")
                farmer.finalState.transportBottleneck > Duration.ZERO ->
                    appendLine("🚛 ${farmer.finalState.transportBottleneck.asDaysHoursAndMinutes(compact)} ")
            }
            when {
                farmer.awayTimeRemaining < Duration.ZERO ->
                    appendLine("💤 Sleeping!")
                farmer.awayTimeRemaining < Duration.standardHours(12L) ->
                    appendLine("💤 ${farmer.awayTimeRemaining.asDaysHoursAndMinutes(compact)}")
            }
            this@message.appendLine("```")
        }
    }

    // endregion Bottlenecks

}.toString()
