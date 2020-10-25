package nl.pindab0ter.eggbot.view

import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.model.EarningsBonus
import nl.pindab0ter.eggbot.model.EarningsBonus.Companion.MAX_PROPHECY_EGG_RESEARCH_LEVEL
import nl.pindab0ter.eggbot.model.EarningsBonus.Companion.MAX_SOUL_EGG_RESEARCH_LEVEL
import nl.pindab0ter.eggbot.model.Table
import nl.pindab0ter.eggbot.model.database.Farmer
import org.joda.time.Duration


fun earningsBonusResponse(
    farmer: Farmer,
    earningsBonusObject: EarningsBonus,
    timeSinceBackup: Duration,
    compact: Boolean,
    extended: Boolean,
): List<String> = earningsBonusObject.run {
    data class Row(val label: String = "", val value: String = "", val suffix: String = "")

    fun MutableList<Row>.addRow(label: String = "", value: String = "", suffix: String = "") =
        add(Row(label, value, suffix))

    val rows = mutableListOf<Row>().apply {
        addRow("Rank:", earningsBonus.asRank(shortened = compact))
        addRow("Backed up:", timeSinceBackup.asDaysHoursAndMinutes(compact = compact), " ago")
        addRow(
            "Earnings Bonus:",
            if (extended) earningsBonus.formatInteger()
            else earningsBonus.asIllions(shortened = compact), " %"
        )
        addRow(
            "Soul Eggs:",
            if (extended) soulEggs.formatInteger()
            else soulEggs.asIllions(shortened = compact)
        )
        addRow("Prophecy Eggs:", prophecyEggs.formatInteger())
        if (soulEggsResearchLevel < MAX_SOUL_EGG_RESEARCH_LEVEL)
            addRow("Soul Bonus:", soulEggsResearchLevel.formatInteger(), "/140")
        if (prophecyEggsResearchLevel < MAX_PROPHECY_EGG_RESEARCH_LEVEL)
            addRow("Prophecy Bonus:", prophecyEggsResearchLevel.formatInteger(), "/5")
        addRow("Prestiges:", farmer.prestiges.formatInteger())
        addRow(
            "SE to next rank:", "+ ${
                if (extended) soulEggsToNextRank.formatInteger()
                else soulEggsToNextRank.asIllions(shortened = compact)
            }"
        )
        addRow("PE to next rank:", "+ ${prophecyEggsToNextRank.formatInteger()}")
    }

    return table {
        title = "Earnings bonus for **${farmer.inGameName}**"
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
    }
}