package nl.pindab0ter.eggbot.view

import com.auxbrain.ei.Contract
import nl.pindab0ter.eggbot.EggBot.guild
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.BigDecimal.Companion.SIXTY
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

    if (!compact) {
        drawBasicInfo(contract)
        drawCoops(contract, statuses)
    } else {
        drawCompactBasicInfo(contract)
        drawCompactCoops(contract, statuses)
    }

}.splitCodeBlock()

private fun StringBuilder.drawBasicInfo(contract: Contract): StringBuilder = appendTable {
    title = "__**🗒️ Basic info**__"
    displayHeaders = false

    column {
        rightPadding = 1
        cells = listOf(
            "Contract:",
            "Final goal:",
            "Time to complete:",
            "Max size:",
        )
    }

    column {
        cells = listOf(
            contract.name,
            contract.finalGoal.formatIllions(OPTIONAL_DECIMALS),
            contract.lengthSeconds.toDuration().formatDaysAndHours(),
            "${contract.maxCoopSize} farmers",
        )
    }
}


private fun StringBuilder.drawCompactBasicInfo(contract: Contract): StringBuilder = appendTable {
    title = "__**🗒️ Basic info**__"
    displayHeaders = false

    column {
        rightPadding = 2
        cells = listOf(
            "Contract:",
            "Final goal:",
            "Time to complete:",
            "Max size:",
        )
    }

    column {
        alignment = RIGHT
        cells = listOf(
            contract.name,
            contract.finalGoal.formatIllions(OPTIONAL_DECIMALS),
            contract.lengthSeconds.toDuration().formatDaysAndHours(),
            "${contract.maxCoopSize} farmers",
        )
    }
}

private fun StringBuilder.drawCoops(
    contract: Contract,
    statuses: List<CoopContractStatus>,
): StringBuilder = appendTable {

    title = "__**🐓 Co-ops** (${statuses.count()})__"
    topPadding = 1

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

    column {
        header = "Status"

        alignment = RIGHT
        leftPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is NotFound -> "Waiting for starter"
                is Abandoned -> "Abandoned"
                is Failed -> "Failed"
                is NotOnTrack -> "${status.state.timeUpPercentageOfFinalGoal.formatTwoDecimals()}%"
                is OnTrack -> status.state.timeTillFinalGoal?.formatDaysHoursAndMinutes(compact = true, spacing = true)
                    ?: "ERROR"
                is FinishedIfBanked -> "Bank now!"
                is Finished -> "Finished"
            }
        }
    }

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
                is FinishedIfBanked -> "🔵"
                is Finished -> "🏁"
            }
        }
    }

    column {
        header = "${'#'.repeat(contract.maxCoopSize.toString().length)}/${contract.maxCoopSize}"

        alignment = RIGHT
        rightPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is NotFound -> ""
                is InActive -> "${status.coopStatus.contributors.count()}/${contract.maxCoopSize}"
                is InProgress -> "${status.state.farmers.count()}/${contract.maxCoopSize}"
            }
        }
    }

    column {
        header = "Current"

        alignment = RIGHT
        leftPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is InProgress -> status.state.currentEggsLaid.formatIllions()
                else -> ""
            }
        }
    }

    divider()

    column {
        header = "Banked"

        alignment = RIGHT
        leftPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is Failed -> status.coopStatus.eggsLaid.formatIllions()
                is InProgress -> status.state.reportedEggsLaid.formatIllions()
                else -> ""
            }
        }
    }

    divider()

    overtakersColumn(statuses)

    column {
        header = "Eggs/hr"

        alignment = LEFT

        cells = statuses.map { status ->
            when (status) {
                is InProgress -> status.state.currentEggsPerMinute.times(SIXTY).formatIllions()
                else -> ""
            }
        }
    }
}

private fun StringBuilder.drawCompactCoops(
    contract: Contract,
    statuses: List<CoopContractStatus>,
): StringBuilder = appendTable {

    title = "__**🐓 Co-ops** (${statuses.count()})__"
    topPadding = 1

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

    column {
        header = "Status"

        alignment = RIGHT
        leftPadding = 1

        cells = statuses.map { status ->
            when (status) {
                is NotFound -> "Start"
                is Abandoned -> "Empty"
                is Failed -> "Failed"
                is NotOnTrack -> "${status.state.timeUpPercentageOfFinalGoal.formatTwoDecimals()}%"
                is OnTrack -> status.state.timeTillFinalGoal?.formatHourAndMinutes() ?: "ERROR"
                is FinishedIfBanked -> "Bank!"
                is Finished -> "Done"
            }
        }
    }

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
                is FinishedIfBanked -> "🔵"
                is Finished -> "🏁"
            }
        }
    }

    column {
        header = "${'#'.repeat(contract.maxCoopSize.toString().length)}/${contract.maxCoopSize}"

        alignment = RIGHT

        cells = statuses.map { status ->
            when (status) {
                is NotFound -> ""
                is InActive -> "${status.coopStatus.contributors.count()}/${contract.maxCoopSize}"
                is InProgress -> "${status.state.farmers.count()}/${contract.maxCoopSize}"
            }
        }
    }
}

private fun Table.overtakersColumn(statuses: List<CoopContractStatus>, init: Table.Column.() -> Unit = {}) {
    fun CoopContractStatus.currentEggsLaid(): BigDecimal = when (this) {
        is InProgress -> state.currentEggsLaid
        else -> ZERO
    }

    fun CoopContractStatus.timeUpEggsLaid(): BigDecimal = when (this) {
        is InProgress -> state.timeUpEggsLaid
        else -> ZERO
    }

    fun CoopContractStatus.willOvertake(): Boolean = statuses.minus(this).any { other ->
        this.currentEggsLaid() < other.currentEggsLaid() && this.timeUpEggsLaid() > other.timeUpEggsLaid()
    }

    fun CoopContractStatus.isSlowerThanAny(): Boolean = statuses.minus(this).any { other ->
        this.currentEggsLaid() > other.currentEggsLaid() && this.timeUpEggsLaid() < other.timeUpEggsLaid()
    }

    val overtakers: List<String> = statuses.map { status ->
        when {
            status.willOvertake() -> "↑"
            status.isSlowerThanAny() -> "↓"
            else -> " "
        }
    }

    if (overtakers.any { it != " " }) column {
        cells = overtakers

        leftPadding = 1
        rightPadding = 1

        init()
    }
}
