package nl.pindab0ter.eggbot.view

import com.auxbrain.ei.Backup
import nl.pindab0ter.eggbot.MAX_PROPHECY_EGG_RESEARCH_LEVEL
import nl.pindab0ter.eggbot.MAX_SOUL_EGG_RESEARCH_LEVEL
import nl.pindab0ter.eggbot.NO_ALIAS
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.DisplayMode.COMPACT
import nl.pindab0ter.eggbot.helpers.DisplayMode.EXTENDED
import nl.pindab0ter.eggbot.model.Table


fun earningsBonusResponse(
    farmer: Backup,
    displayMode: DisplayMode,
): String = farmer.game.run {
    data class Row(val label: String = "", val value: String = "", val suffix: String = "")

    fun MutableList<Row>.addRow(label: String = "", value: String = "", suffix: String = "") = add(Row(label, value, suffix))
    val compact = displayMode == COMPACT

    val rows = mutableListOf<Row>().apply {
        addRow("Rank:", earningsBonus.formatRank(shortened = compact))
        addRow("Backed up:", farmer.timeSinceBackup.formatDaysHoursAndMinutes(compact = compact), " ago")
        addRow(
            "Earnings Bonus:",
            when (displayMode) {
                EXTENDED -> earningsBonus.formatInteger()
                else -> earningsBonus.formatIllions(shortened = compact)
            },
            "%"
        )
        addRow(
            "Soul Eggs:",
            when (displayMode) {
                EXTENDED -> soulEggs.toString()
                else -> soulEggs.toBigDecimal().formatIllions(shortened = compact)
            }
        )
        addRow("Prophecy Eggs:", prophecyEggs.formatInteger())
        if (soulEggResearchLevel < MAX_SOUL_EGG_RESEARCH_LEVEL)
            addRow("Soul Bonus:", soulEggResearchLevel.formatInteger(), "/140")
        if (prophecyEggResearchLevel < MAX_PROPHECY_EGG_RESEARCH_LEVEL)
            addRow("Prophecy Bonus:", prophecyEggResearchLevel.formatInteger(), "/5")
        addRow("Prestiges:", farmer.stats.prestiges.formatInteger())
        addRow("Rockets launched:", farmer.rocketsLaunched.formatInteger())
        addRow(
            "SE to next rank:", "+ ${
                when (displayMode) {
                    EXTENDED -> soulEggsToNextRank.formatInteger()
                    else -> soulEggsToNextRank.formatIllions(shortened = compact)
                }
            }"
        )
        addRow("PE to next rank:", "+ ${prophecyEggsToNextRank.formatInteger()}")
    }

    return table {
        title = "Earnings bonus for **${if (!farmer.userName.isNullOrBlank()) farmer.userName else NO_ALIAS}**"
        displayHeaders = false
        column {
            rightPadding = 2
            cells = rows.map { row -> row.label }
        }
        column {
            alignment = Table.AlignedColumn.Alignment.RIGHT
            cells = rows.map { row -> row.value }
        }
        column {
            cells = rows.map { row -> row.suffix }
        }
    }.first()
}