package nl.pindab0ter.eggbot.view

import com.auxbrain.ei.Contract
import nl.pindab0ter.eggbot.EggBot.guild
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.NumberFormatter.OPTIONAL_DECIMALS
import nl.pindab0ter.eggbot.model.Table
import nl.pindab0ter.eggbot.model.Table.AlignedColumn.Alignment.LEFT
import nl.pindab0ter.eggbot.model.Table.AlignedColumn.Alignment.RIGHT
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.*
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.InActive.*
import nl.pindab0ter.eggbot.model.simulation.CoopContractStatus.InProgress.*
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import kotlin.random.Random


fun coopsInfoResponse(
    contract: Contract,
    statuses: List<CoopContractStatus>,
    compact: Boolean,
) = buildString {
    appendLine("`${guild.name}` vs. _${contract.name}_:")
    appendLine()

    drawBasicInfo(contract)
    if (!compact) drawCoops(contract, statuses)
    else drawCompactCoops(contract, statuses)

}.splitCodeBlock()

private fun StringBuilder.drawBasicInfo(contract: Contract): StringBuilder = apply {
    appendLine("__🗒️ **Basic info**__ ```")
    appendLine("Contract:         ${contract.name}")
    appendLine("Final goal:       ${contract.finalGoal.asIllions(OPTIONAL_DECIMALS)}")
    appendLine("Time to complete: ${contract.lengthSeconds.toDuration().asDaysHoursAndMinutes(true)}")
    appendLine("Max size:         ${contract.maxCoopSize} farmers")
    appendLine("```")
}

private fun StringBuilder.drawCoops(
    contract: Contract,
    statuses: List<CoopContractStatus>,
): StringBuilder = appendTable {

    // TODO: Replace "eggs" column with "reported" column?

    incrementColumn {
        suffix = "."
        displayRows = statuses.map { status ->
            when (status) {
                is NotFound, is Abandoned -> false
                else -> true
            }
        }
    }

    column {
        header = "Name"

        leftPadding = 1
        rightPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is NotFound -> status.coopId
                is InActive -> status.coopStatus.coopId
                is InProgress -> status.state.coopId
            }
        }
    }

    divider()

    emojiColumn {
        header = "🚥"

        leftPadding = 1
        rightPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is NotFound -> "🟡"
                is Abandoned -> "🔴"
                is Failed -> "🔴"
                is NotOnTrack -> "🟡"
                is OnTrack -> "🟢"
                is FinishedIfCheckedIn -> "🟢"
                is Finished -> "🏁"
            }
        }
    }

    column {
        header = "Status"

        rightPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is NotFound -> "Waiting for starter"
                is Abandoned -> "Abandoned"
                is Failed -> "Failed"
                is NotOnTrack -> "Short ${
                    "(${status.state.timeUpPercentageOfFinalGoal.formatTwoDecimals()}%)".padStart(7, ' ')
                }"
                is OnTrack -> "On Track"
                is FinishedIfCheckedIn -> "Finished if checked in"
                is Finished -> "Finished"
            }
        }
    }

    column {
        header = "Eggs"

        alignment = RIGHT
        leftPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is Failed -> status.coopStatus.eggsLaid.asIllions()
                is InProgress -> status.state.currentEggsLaid.asIllions()
                else -> ""
            }
        }
    }

    overtakersColumn(statuses) {
        leftPadding = 1
    }

    divider()

    column {
        header = "/hr"

        alignment = LEFT
        rightPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is InProgress -> status.state.currentEggsPerMinute.asIllions()
                else -> ""
            }
        }
    }

    column {
        header = "Finished In"

        alignment = RIGHT
        leftPadding = 1
        rightPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is InProgress -> status.state.timeTillFinalGoal?.asDaysHoursAndMinutes(true) ?: "ERROR"
                else -> ""
            }
        }
    }

    divider()

    column {
        header = "${'#'.repeat(contract.maxCoopSize.toString().length)}/${contract.maxCoopSize}"

        alignment = RIGHT
        leftPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is NotFound -> ""
                is InActive -> "${status.coopStatus.contributors.count()}/${contract.maxCoopSize}"
                is InProgress -> "${status.state.farmers.count()}/${contract.maxCoopSize}"
            }
        }
    }
}

private fun StringBuilder.drawCompactCoops(
    contract: Contract,
    statuses: List<CoopContractStatus>,
): StringBuilder = appendTable {
    column {
        header = "Name"

        rightPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is NotFound -> status.coopId
                is InActive -> status.coopStatus.coopId
                is InProgress -> status.state.coopId
            }
        }
    }

    divider()

    emojiColumn {
        header = "🚥"

        leftPadding = 1
        rightPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is NotFound -> "🟡"
                is Abandoned -> "🔴"
                is Failed -> "🔴"
                is NotOnTrack -> "🔴"
                is OnTrack -> "🟢"
                is FinishedIfCheckedIn -> "🟢"
                is Finished -> "🏁"
            }
        }
    }

    divider()

    column {
        header = "Eggspected"

        leftPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is Failed -> status.coopStatus.eggsLaid.asIllions()
                is InProgress -> status.state.timeUpEggsLaid.asIllions()
                else -> ""
            }
        }
    }

    column {
        header = "${'#'.repeat(contract.maxCoopSize.toString().length)}/${contract.maxCoopSize}"

        alignment = RIGHT
        leftPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is NotFound -> ""
                is InActive -> "${status.coopStatus.contributors.count()}/${contract.maxCoopSize}"
                is InProgress -> "${status.state.farmers.count()}/${contract.maxCoopSize}"
            }
        }
    }
}

private fun Table.overtakersColumn(statuses: List<CoopContractStatus>, init: Table.EmojiColumn.() -> Unit) {
    fun CoopContractStatus.eggIncreasePerMinute(): BigDecimal = when (this) {
        is InProgress -> state.currentEggsPerMinute
        else -> ZERO
    }

    fun CoopContractStatus.eggsLaid(): BigDecimal = when (this) {
        is InProgress -> state.currentEggsLaid
        else -> ZERO
    }

    // TODO: currentEggs vs. finalEggs instead, i.e.: Will somebody actually overtake someone in time?
    // TODO: isFasterThanAny → willOvertake
    fun CoopContractStatus.isFasterThanAny(): Boolean = statuses.minus(this).any { other ->
        this.eggsLaid() < other.eggsLaid() && this.eggIncreasePerMinute() > other.eggIncreasePerMinute()
    }

    // TODO: isSlowerThanAny → willBeOvertaken
    fun CoopContractStatus.isSlowerThanAny(): Boolean = statuses.minus(this).any { other ->
        this.eggsLaid() > other.eggsLaid() && this.eggIncreasePerMinute() < other.eggIncreasePerMinute()
    }

    val overtakers: List<String> = statuses.map { status ->
        when {
            status.isFasterThanAny() -> "⬆️"
            status.isSlowerThanAny() -> "⬇️"
            else -> "➖"
        }
    }

    if (overtakers.any { it != "➖" }) emojiColumn {
        header = if (Random.nextBoolean()) "🏃‍♂️" else "🏃‍♀️"
        cells = overtakers
        init()
    }
}
