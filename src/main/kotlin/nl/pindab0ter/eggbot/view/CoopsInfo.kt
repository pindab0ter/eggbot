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

        leftPadding = 1
        rightPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is Failed -> status.coopStatus.eggsLaid.asIllions()
                is InProgress -> status.state.finalEggsLaid.asIllions()
                else -> ""
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
                is InProgress -> status.state.initialEggsLaid.asIllions()
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
                is NotOnTrack -> "🟡"
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
                is InProgress -> status.state.finalEggsLaid.asIllions()
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
        is InProgress -> state.farmers.sumByBigDecimal { farmer ->
            eggIncrease(farmer.initialState.habs, farmer.constants)
        }
        else -> ZERO
    }

    fun CoopContractStatus.eggsLaid(): BigDecimal = when (this) {
        is InProgress -> state.initialEggsLaid
        else -> ZERO
    }

    fun CoopContractStatus.isFasterThanAny(): Boolean = statuses.minus(this).any { other ->
        this.eggsLaid() < other.eggsLaid() && this.eggIncreasePerMinute() > other.eggIncreasePerMinute()
    }

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
