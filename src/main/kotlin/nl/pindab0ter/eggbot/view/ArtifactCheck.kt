package nl.pindab0ter.eggbot.view

import com.auxbrain.ei.Backup
import com.auxbrain.ei.Backup.Farm
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.NumberFormatter.THREE_DECIMALS
import nl.pindab0ter.eggbot.model.Table.AlignedColumn.Alignment.RIGHT
import nl.pindab0ter.eggbot.model.auxbrain.*
import java.math.BigDecimal

fun artifactCheckResponse(farm: Farm, backup: Backup): List<String> {
    data class Row(val label: String = "", val value: String = "", val suffix: String = "")

    fun MutableList<Row>.addRow(label: String = "", value: String = "", suffix: String = "") =
        add(Row(label, value, suffix))

    val habRows = buildList {
        addRow(
            label = "Base capacity",
            value = farm.baseHabCapacity
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

        backup.artifactsFor(farm).filter { artifact ->
            artifact.name in habCapacityArtifacts
        }.forEach { habCapacityArtifact ->
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
            value = farm.baseHabCapacity
                .times(backup.habCapacityMultiplierFor(farm))
                .formatIllions(THREE_DECIMALS),
            suffix = "🐔"
        )
    }

    val transportRows = buildList {
        addRow(
            label = "Base capacity",
            value = farm.baseShippingRate
                .times(backup.shippingRateResearchMultiplierFor(farm))
                .formatIllions(THREE_DECIMALS),
            suffix = "🥚/min"
        )

        addRow()

        addRow(
            label = "Artifact multiplier:",
            value = backup.shippingRateArtifactsMultiplierFor(farm).formatPercentage(),
            suffix = "%"
        )

        backup.artifactsFor(farm).filter { artifact ->
            artifact.name in shippingRateArtifacts
        }.forEach { habCapacityArtifact ->
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
            value = backup.shippingRateFor(farm).formatIllions(THREE_DECIMALS),
            suffix = "🥚/min"
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
                cells = habRows.map(Row::label)
            }
            column {
                alignment = RIGHT
                cells = habRows.map(Row::value)
            }
            column {
                leftPadding = 1
                cells = habRows.map(Row::suffix)
            }
        }
        appendBreakpoint()
        appendTable {
            title = "__**🚛 Transport capacity**__"
            displayHeaders = false
            topPadding = 1
            column {
                rightPadding = 2
                cells = transportRows.map(Row::label)
            }
            column {
                alignment = RIGHT
                cells = transportRows.map(Row::value)
            }
            column {
                leftPadding = 1
                cells = transportRows.map(Row::suffix)
            }
        }

    }.splitMessage(separator = BREAKPOINT)
}
