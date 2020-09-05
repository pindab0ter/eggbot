package nl.pindab0ter.eggbot.view

import com.auxbrain.ei.Contract
import nl.pindab0ter.eggbot.EggBot.guild
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.NumberFormatter.OPTIONAL_DECIMALS
import nl.pindab0ter.eggbot.model.Table
import nl.pindab0ter.eggbot.model.Table.AlignedColumn.Alignment.LEFT
import nl.pindab0ter.eggbot.model.Table.AlignedColumn.Alignment.RIGHT
import nl.pindab0ter.eggbot.model.simulation.new.CoopContractStatus
import nl.pindab0ter.eggbot.model.simulation.new.CoopContractStatus.*
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
                is Abandoned -> status.coopStatus.coopId
                is Failed -> status.coopStatus.coopId
                is NotOnTrack -> status.state.coopId
                is OnTrack -> status.state.coopId
                is FinishedIfCheckedIn -> status.state.coopId
                is Finished -> status.coopStatus.coopId
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
                is NotFound -> ""
                is Abandoned -> ""
                is Failed -> status.coopStatus.eggsLaid.asIllions()
                is NotOnTrack -> status.state.eggsLaid.asIllions()
                is OnTrack -> status.state.eggsLaid.asIllions()
                is FinishedIfCheckedIn -> status.state.eggsLaid.asIllions()
                is Finished -> ""
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
                is NotFound -> ""
                is Abandoned -> ""
                is Failed -> ""
                is NotOnTrack -> status.state.farmers.sumByBigDecimal { it.initialState.eggsLaid }.asIllions()
                is OnTrack -> status.state.farmers.sumByBigDecimal { it.initialState.eggsLaid }.asIllions()
                is FinishedIfCheckedIn -> status.state.farmers.sumByBigDecimal { it.initialState.eggsLaid }.asIllions()
                is Finished -> ""
            }
        }
    }

    divider()

    column {
        header = "${'#'.repeat(contract.maxCoopSize).length}/${contract.maxCoopSize}"

        alignment = RIGHT
        leftPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is NotFound -> ""
                is Abandoned -> "${status.coopStatus.contributors.count()}/${contract.maxCoopSize}"
                is Failed -> "${status.coopStatus.contributors.count()}/${contract.maxCoopSize}"
                is NotOnTrack -> "${status.state.farmers.count()}/${contract.maxCoopSize}"
                is OnTrack -> "${status.state.farmers.count()}/${contract.maxCoopSize}"
                is FinishedIfCheckedIn -> "${status.state.farmers.count()}/${contract.maxCoopSize}"
                is Finished -> "${status.coopStatus.contributors.count()}/${contract.maxCoopSize}"
            }
        }
    }
}

private fun Table.overtakersColumn(statuses: List<CoopContractStatus>, init: Table.EmojiColumn.() -> Unit) {
    fun CoopContractStatus.eggIncreasePerMinute(): BigDecimal = when (this) {
        is NotFound -> ZERO
        is Abandoned -> ZERO
        is Failed -> ZERO
        is NotOnTrack -> state.farmers.sumByBigDecimal { farmer ->
            eggIncrease(farmer.finalState.habs, farmer.constants)
        }
        is OnTrack -> state.farmers.sumByBigDecimal { farmer ->
            eggIncrease(farmer.finalState.habs, farmer.constants)
        }
        is FinishedIfCheckedIn -> state.farmers.sumByBigDecimal { farmer ->
            eggIncrease(farmer.finalState.habs, farmer.constants)
        }
        is Finished -> ZERO
    }

    fun CoopContractStatus.eggsLaid(): BigDecimal = when (this) {
        is NotFound -> ZERO
        is Abandoned -> ZERO
        is Failed -> ZERO
        is NotOnTrack -> state.eggsLaid
        is OnTrack -> state.eggsLaid
        is FinishedIfCheckedIn -> state.eggsLaid
        is Finished -> ZERO
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
