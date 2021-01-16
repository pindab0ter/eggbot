package nl.pindab0ter.eggbot.view

import com.auxbrain.ei.Backup
import com.auxbrain.ei.Backup.Farm
import com.auxbrain.ei.HabLevel
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.model.Table
import nl.pindab0ter.eggbot.model.auxbrain.*
import java.math.BigDecimal

fun artifactCheckResponse(farm: Farm, backup: Backup): List<String> {
    data class Row(val label: String = "", val value: String = "", val suffix: String = "")

    fun MutableList<Row>.addRow(label: String = "", value: String = "", suffix: String = "") =
        add(Row(label, value, suffix))

    val habCapacityArtifacts = backup.artifactsFor(farm).filter { artifact ->
        artifact.name in habCapacityArtifacts
    }
    val rows = buildList {
        addRow(
            label = "Base capacity",
            value = (farm.habs.sumOf(HabLevel::capacity))
                .times(farm.habCapacityResearchMultiplier())
                .formatInteger(),
            suffix = "🐔"
        )

        addRow()

        addRow(
            label = "Artifact multiplier:",
            value = backup.habCapacityArtifactsMultiplierFor(farm).formatPercentage(),
            suffix = "%"
        )

        habCapacityArtifacts.forEach { habCapacityArtifact ->
            val multiplier = habCapacityArtifact.multiplier
            addRow(
                label = "  + ${habCapacityArtifact.fullName}",
                value = if (multiplier != BigDecimal.ONE) multiplier.formatPlusPercentage() else "Unknown",
                suffix = if (multiplier != BigDecimal.ONE) "%" else ""
            )
        }

        addRow()

        addRow(
            label = "Total capacity",
            value = (farm.habs.sumOf(HabLevel::capacity))
                .times(backup.habCapacityMultiplierFor(farm))
                .formatInteger(),
            suffix = "🐔"
        )
    }

    return buildString {
        appendLine("**${backup.userName}**’s home farm:")
        appendLine()
        appendTable {
            title = "__**🏠 Hab capacity:**__"
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
                leftPadding = 1
                cells = rows.map { row -> row.suffix }
            }
        }
    }.splitMessage(separator = BREAKPOINT)
}
