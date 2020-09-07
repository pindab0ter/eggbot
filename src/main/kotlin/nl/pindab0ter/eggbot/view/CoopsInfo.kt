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
) = buildString {
    appendLine("`${guild.name}` vs. _${contract.name}_:")
    appendLine()

    drawBasicInfo(contract)
    drawCoops(contract, statuses)
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
                is NotOnTrack -> "🟡"
                is OnTrack -> "🟢"
                is FinishedIfCheckedIn -> "🟢"
                is Finished -> "🏁"
            }
        }
    }

    divider()

    column {
        header = "Status"

        leftPadding = 1
        rightPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is NotFound -> "Waiting for starter"
                is Abandoned -> "Abandoned"
                is Failed -> "Failed"
                is NotOnTrack -> "Not on track"
                is OnTrack -> "On Track"
                is FinishedIfCheckedIn -> "Finished if checked in"
                is Finished -> "Finished"
            }
        }
    }

    divider()

    column {
        header = "Eggspected"

        alignment = RIGHT
        leftPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is Failed -> status.coopStatus.eggsLaid.asIllions()
                is InProgress -> status.state.eggspected.asIllions()
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
                is InProgress -> status.state.farmers.sumByBigDecimal { farmer ->
                    eggIncrease(farmer.initialState.habs, farmer.constants)
                }.asIllions()
                else -> ""
            }
        }
    }

    column {
        header = "Time required"

        alignment = RIGHT
        leftPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is InProgress ->
                    if (status.state.elapsed >= ONE_YEAR) "> 1yr"
                    else status.state.elapsed.asDaysHoursAndMinutes(true)
                else -> ""
            }
        }
    }

    divider()

    column {
        header = "/remaining"

        alignment = LEFT
        rightPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is InProgress -> status.state.timeRemaining.asDaysHoursAndMinutes(true)
                else -> ""
            }
        }
    }

    column {
        header = "Current %"

        alignment = RIGHT

        leftPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is InActive -> "${
                    (status.coopStatus.eggsLaid / contract.finalGoal).times(BigDecimal(100)).formatTwoDecimals()
                } %"
                is InProgress -> "${
                    (status.state.initialEggsLaid / contract.finalGoal).times(BigDecimal(100)).formatTwoDecimals()
                } %"
                else -> ""
            }
        }
    }

    divider()

    column {
        header = "/eggspected"

        alignment = LEFT

        rightPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is InProgress -> "${
                    (status.state.eggspected / contract.finalGoal).times(BigDecimal(100)).formatTwoDecimals()
                } %"
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

private fun Table.overtakersColumn(statuses: List<CoopContractStatus>, init: Table.EmojiColumn.() -> Unit) {
    fun CoopContractStatus.eggIncreasePerMinute(): BigDecimal = when (this) {
        is InProgress -> state.farmers.sumByBigDecimal { farmer ->
            eggIncrease(farmer.finalState.habs, farmer.constants)
        }
        else -> ZERO
    }

    fun CoopContractStatus.eggsLaid(): BigDecimal = when (this) {
        is InProgress -> state.finalEggsLaid
        else -> ZERO
    }

    fun CoopContractStatus.isFasterThanAny(): Boolean = statuses.any { status ->
        eggsLaid() < status.eggsLaid() && eggIncreasePerMinute() > status.eggIncreasePerMinute()
    }

    fun CoopContractStatus.isSlowerThanAny(): Boolean = statuses.any { status ->
        eggsLaid() > status.eggsLaid() && eggIncreasePerMinute() < status.eggIncreasePerMinute()
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
