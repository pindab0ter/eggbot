package nl.pindab0ter.eggbot.view

import com.auxbrain.ei.Contract
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.BigDecimal.Companion.SIXTY
import nl.pindab0ter.eggbot.helpers.NumberFormatter.OPTIONAL_DECIMALS
import nl.pindab0ter.eggbot.helpers.Typography.zwsp
import nl.pindab0ter.eggbot.model.Table
import nl.pindab0ter.eggbot.model.Table.AlignedColumn.Alignment.RIGHT
import nl.pindab0ter.eggbot.model.simulation.SoloContractState
import kotlin.random.Random


fun solosInfoResponse(
    contract: Contract,
    states: List<SoloContractState>,
    compact: Boolean,
) = buildString {
    appendLine("`` vs. _${contract.name}_:")
    appendLine("`${configuredGuild?.name}` vs. _${contract.name}_:")
    appendLine()

    if (!compact) {
        drawBasicInfo(contract)
        appendBreakpoint()
        drawFarmers(states.sortedWith(SoloContractState.timeUpEggsLaidComparator))
    } else {
        drawCompactBasicInfo(contract)
        appendBreakpoint()
        drawCompactFarmers(states.sortedWith(SoloContractState.timeUpEggsLaidComparator))
    }

}.splitMessage(separator = zwsp)

private fun StringBuilder.drawBasicInfo(contract: Contract): StringBuilder = appendTable {
    title = "__**🗒️ Basic info:**__"
    displayHeaders = false

    column {
        rightPadding = 1
        cells = listOf(
            "Contract:",
            "Final goal:",
            "Time to complete:",
        )
    }

    column {
        cells = listOf(
            contract.name,
            contract.finalGoal.formatIllions(OPTIONAL_DECIMALS),
            contract.lengthSeconds.toDuration().formatDaysAndHours(),
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
        )
    }

    column {
        alignment = RIGHT
        cells = listOf(
            contract.name,
            contract.finalGoal.formatIllions(OPTIONAL_DECIMALS),
            contract.lengthSeconds.toDuration().formatDaysAndHours(),
        )
    }
}

private fun StringBuilder.drawFarmers(
    states: List<SoloContractState>,
): StringBuilder = appendTable {
    val farmerEmoji = when (Random.nextBoolean()) {
        true -> "👨‍🌾"
        false -> "👩‍🌾"
    }

    title = "__**$farmerEmoji Farmers** (${states.count()})__"
    topPadding = 1

    column {
        header = "Name"

        rightPadding = 1

        cells = states.map { state ->
            state.farmer.name
        }
    }

    column {
        header = "Status"

        alignment = RIGHT
        leftPadding = 1

        cells = states.map { state ->
            when {
                state.willFinish ->
                    state.timeTillFinalGoal?.formatDaysHoursAndMinutes(compact = true, spacing = true) ?: "ERROR"
                state.finishedIfBanked ->
                    "Bank now!"
                state.finished ->
                    "Finished"
                else ->
                    "${state.timeUpPercentageOfFinalGoal.formatTwoDecimals()}%"
            }
        }
    }

    emojiColumn {
        header = "🚥"

        leftPadding = 1
        rightPadding = 1

        cells = states.map { state ->
            when {
                state.willFinish -> "🟢"
                state.finishedIfBanked -> "🔵"
                state.finished -> "🏁"
                else -> "🟡"
            }
        }
    }

    column {
        header = "Current"

        alignment = RIGHT
        leftPadding = 1

        cells = states.map { status -> status.currentEggsLaid.formatIllions() }
    }

    divider()

    column {
        header = "Banked"

        alignment = RIGHT
        leftPadding = 1

        cells = states.map { state -> state.reportedEggsLaid.formatIllions() }
    }

    divider()

    overtakersColumn(states)

    column {
        header = "Eggs/hr"

        alignment = RIGHT
        leftPadding = 1

        cells = states.map { state -> state.currentEggsPerMinute.times(SIXTY).formatIllions() }
    }

    divider(intersection = '╡')
}

private fun StringBuilder.drawCompactFarmers(
    states: List<SoloContractState>,
): StringBuilder = appendTable {
    val farmerEmoji = when (Random.nextBoolean()) {
        true -> "👨‍🌾"
        false -> "👩‍🌾"
    }

    title = "__**$farmerEmoji Co-ops** (${states.count()})__"
    topPadding = 1

    column {
        header = "Name"

        rightPadding = 1

        cells = states.map { state ->
            state.farmer.name.let { name ->
                if (name.length <= 10) name
                else "${name.substring(0 until 9)}…"
            }
        }
    }

    column {
        header = "Status"

        alignment = RIGHT
        leftPadding = 1

        cells = states.map { state ->
            when {
                state.willFinish ->
                    state.timeTillFinalGoal?.formatDaysHoursAndMinutes(compact = true, spacing = true) ?: "ERROR"
                state.finishedIfBanked ->
                    "Bank now!"
                state.finished ->
                    "Finished"
                else ->
                    "${state.timeUpPercentageOfFinalGoal.formatTwoDecimals()}%"
            }
        }
    }

    emojiColumn {
        header = "🚥"

        leftPadding = 1
        rightPadding = 1

        cells = states.map { state ->
            when {
                state.willFinish -> "🟢"
                state.finishedIfBanked -> "🔵"
                state.finished -> "🏁"
                else -> "🟡"
            }
        }
    }
}

private fun Table.overtakersColumn(states: List<SoloContractState>, init: Table.EmojiColumn.() -> Unit = {}) {
    fun SoloContractState.willOvertake(): Boolean = states.minus(this).any { other ->
        this.currentEggsLaid < other.currentEggsLaid && this.timeUpEggsLaid > other.timeUpEggsLaid
    }

    fun SoloContractState.isSlowerThanAny(): Boolean = states.minus(this).any { other ->
        this.currentEggsLaid > other.currentEggsLaid && this.timeUpEggsLaid < other.timeUpEggsLaid
    }

    val overtakers: List<String> = states.map { status ->
        when {
            status.willOvertake() -> "⬆️"
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
