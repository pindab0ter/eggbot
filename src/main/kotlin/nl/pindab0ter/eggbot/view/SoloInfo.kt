package nl.pindab0ter.eggbot.view

import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.model.Config
import nl.pindab0ter.eggbot.model.simulation.new.SoloContractState
import org.joda.time.Duration


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
    contract.farmer.finalState.apply {
        this@message.append("Eggspected:       ${contract.eggspected.asIllions()} ")
        if (!compact) append("(${contract.farmer.finalState.let { eggIncrease(it.habs, it.constants) }.asIllions()}/hr) ")
        this@message.appendLine()
        this@message.appendLine("Time remaining:   ${contract.timeRemaining.asDaysHoursAndMinutes(compact)}")
        append("Current chickens: ${population.asIllions()} ")
        if (!compact) append("(${constants.internalHatcheryRate.asIllions()}/hr)")
        this@message.appendLine()
        append("Current eggs:     ${contract.farmer.initialState.eggsLaid.asIllions()} ")
        if (!compact) append("(${contract.farmer.initialState.let { eggIncrease(it.habs, it.constants) }.asIllions()}/hr) ")
        this@message.appendLine()
        this@message.appendLine("Last update:      ${contract.farmer.timeSinceBackup.asDaysHoursAndMinutes(compact)} ago")
        this@message.appendLine("```")
    }

    // endregion Basic info and totals

    // region Bottlenecks

    contract.farmer.finalState.apply {
        if (habBottleneck != null || transportBottleneck != null) {
            this@message.appendLine("__**⚠ Bottlenecks**__ ```")
            habBottleneck?.let {
                if (it == Duration.ZERO) append("🏠Full! ")
                else append("🏠${it.asDaysHoursAndMinutes(true)} ")
            }
            transportBottleneck?.let {
                if (it == Duration.ZERO) append("🚛Full! ")
                else append("🚛${it.asDaysHoursAndMinutes(true)} ")
            }
            this@message.appendLine("```")
        }
    }

    // endregion Bottlenecks

}.toString()
