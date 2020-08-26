package nl.pindab0ter.eggbot.view

import nl.pindab0ter.eggbot.helpers.asFarmerRole
import nl.pindab0ter.eggbot.helpers.asIllions
import nl.pindab0ter.eggbot.helpers.formatInteger
import nl.pindab0ter.eggbot.helpers.table
import nl.pindab0ter.eggbot.model.Table
import nl.pindab0ter.eggbot.model.database.Farmer


fun earningsBonusResponse(farmer: Farmer, compact: Boolean, extended: Boolean): List<String> {
    data class Row(val label: String = "", val value: String = "", val suffix: String = "")

    fun MutableList<Row>.addRow(label: String = "", value: String = "", suffix: String = "") =
        add(Row(label, value, suffix))

    val rows = mutableListOf<Row>().apply {
        addRow("Role:", farmer.earningsBonus.asFarmerRole(shortened = compact))
        addRow(
            "Earnings Bonus:",
            if (extended) farmer.earningsBonus.formatInteger()
            else farmer.earningsBonus.asIllions(shortened = compact), " %"
        )
        addRow(
            "Soul Eggs:",
            if (extended) farmer.soulEggs.formatInteger()
            else farmer.soulEggs.asIllions(shortened = compact)
        )
        addRow("Prophecy Eggs:", farmer.prophecyEggs.formatInteger())
        if (farmer.soulEggResearchLevel < 140)
            addRow("Soul Bonus:", farmer.soulEggResearchLevel.formatInteger(), "/140")
        if (farmer.prophecyEggResearchLevel < 5)
            addRow("Prophecy Bonus:", farmer.prophecyEggResearchLevel.formatInteger(), "/5")
        addRow("Prestiges:", farmer.prestiges.formatInteger())
        addRow(
            "SE to next rank:", "+ ${
                if (extended) farmer.seToNextRank.formatInteger()
                else farmer.seToNextRank.asIllions(shortened = compact)
            }"
        )
        addRow("PE to next rank:", "+ ${farmer.peToNextRank.formatInteger()}")
    }

    return table {
        title = "Earnings bonus for **${farmer.inGameName}**"
        displayHeader = false
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