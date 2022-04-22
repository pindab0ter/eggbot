package nl.pindab0ter.eggbot.view

import com.auxbrain.ei.Artifact
import com.auxbrain.ei.Backup
import com.auxbrain.ei.Backup.Farm
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.NumberFormatter.THREE_DECIMALS
import nl.pindab0ter.eggbot.helpers.Typography.zwsp
import nl.pindab0ter.eggbot.model.Table.AlignedColumn.Alignment.RIGHT
import nl.pindab0ter.eggbot.model.auxbrain.*
import java.math.BigDecimal.ONE

fun artifactCheckResponse(farm: Farm, backup: Backup, compact: Boolean = false): List<String> {
    data class Row(val label: String = "", val value: String = "", val suffix: String = "")

    fun MutableList<Row>.addRow(label: String = "", value: String = "", suffix: String = "") =
        add(Row(label, value, suffix))

    fun MutableList<Row>.addArtifactRows(artifacts: List<Artifact.Name>) = backup.artifactsFor(farm)
        .filter { artifact ->
            artifact.name in artifacts
        }.sortedByDescending { artifact ->
            artifact.multiplier
        }.forEach { artifact ->
            if (artifact.multiplier != ONE) addRow(
                label = "  + ${if (compact) artifact.formatName() else artifact.formatFullName()}",
                value = artifact.multiplier.formatPlusPercentage(),
                suffix = "%"
            ) else addRow(
                label = "  ### ${if (compact) artifact.formatName() else artifact.formatFullName()}",
                value = "UNKNOWN ###",
            )
        }

    val eggLayingRateRows = buildList {
        if (backup.artifactsFor(farm).filter { artifact -> artifact.name in eggLayingRateArtifacts }.count() > 0) {
            addRow(
                label = "Base egg laying rate",
                value = farm.habPopulations.sumOf(Long::toBigDecimal)
                    .times(EGG_LAYING_BASE_RATE)
                    .times(backup.eggLayingRateResearchMultiplierFor(farm))
                    .formatIllions(THREE_DECIMALS),
                suffix = "🥚/min"
            )

            addRow()

            addRow(
                label = "Artifact multiplier:",
                value = backup.eggLayingRateArtifactsMultiplierFor(farm).formatPercentage(),
                suffix = "%"
            )

            addArtifactRows(eggLayingRateArtifacts)

            addRow()
        }

        addRow(
            label = "Total egg laying rate",
            value = farm.habPopulations.sumOf(Long::toBigDecimal)
                .times(EGG_LAYING_BASE_RATE)
                .times(backup.eggLayingRateMultiplierFor(farm)).formatIllions(THREE_DECIMALS),
            suffix = "🥚/min"
        )
    }

    val hatcheryRateRows = buildList {
        if (backup.artifactsFor(farm).filter { artifact -> artifact.name in hatcheryRateArtifacts }.count() > 0) {
            addRow(
                label = "Base hatchery rate",
                value = backup.hatcheryRateFromResearchFor(farm)
                    .formatIllions(THREE_DECIMALS),
                suffix = "🐔/min"
            )

            addRow()

            addRow(
                label = "Artifact multiplier:",
                value = backup.hatcheryRateArtifactsMultiplierFor(farm).formatPercentage(),
                suffix = "%"
            )

            addArtifactRows(hatcheryRateArtifacts)

            addRow()
        }

        addRow(
            label = "Total hatchery rate",
            value = backup.hatcheryRateFor(farm).formatIllions(THREE_DECIMALS),
            suffix = "🐔/min"
        )
    }

    val habCapacityRows = buildList {
        if (backup.artifactsFor(farm).filter { artifact -> artifact.name in habCapacityArtifacts }.count() > 0) {
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

            addArtifactRows(habCapacityArtifacts)

            addRow()
        }

        addRow(
            label = "Total capacity",
            value = farm.baseHabCapacity
                .times(backup.habCapacityMultiplierFor(farm))
                .formatInteger(),
            suffix = "🐔"
        )
    }

    val shippingRate = buildList {
        if (backup.artifactsFor(farm).filter { artifact -> artifact.name in shippingRateArtifacts }.count() > 0) {
            addRow(
                label = "Base shipping rate",
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

            addArtifactRows(shippingRateArtifacts)

            addRow()
        }

        addRow(
            label = "Total shipping rate",
            value = backup.shippingRateFor(farm).formatIllions(THREE_DECIMALS),
            suffix = "🥚/min"
        )
    }

    return buildString {
        appendLine(
            "**${backup.userName}**’s home farm (last updated ${
                backup.timeSinceBackup.formatDaysHoursAndMinutes(compact = true, spacing = true)
            } ago):"
        )
        appendTable {
            title = "__**🥚 Egg laying rate:**__"
            displayHeaders = false
            topPadding = 1
            column {
                rightPadding = 2
                cells = eggLayingRateRows.map(Row::label)
            }
            column {
                alignment = RIGHT
                cells = eggLayingRateRows.map(Row::value)
            }
            column {
                leftPadding = 1
                cells = eggLayingRateRows.map(Row::suffix)
            }
        }
        appendBreakpoint()

        appendTable {
            title = "__**🐔 Internal hatchery rate:**__"
            displayHeaders = false
            topPadding = 1
            column {
                rightPadding = 2
                cells = hatcheryRateRows.map(Row::label)
            }
            column {
                alignment = RIGHT
                cells = hatcheryRateRows.map(Row::value)
            }
            column {
                leftPadding = 1
                cells = hatcheryRateRows.map(Row::suffix)
            }
        }
        appendBreakpoint()

        appendTable {
            title = "__**🏠 Hab capacity:**__"
            displayHeaders = false
            topPadding = 1
            column {
                rightPadding = 2
                cells = habCapacityRows.map(Row::label)
            }
            column {
                alignment = RIGHT
                cells = habCapacityRows.map(Row::value)
            }
            column {
                leftPadding = 1
                cells = habCapacityRows.map(Row::suffix)
            }
        }
        appendBreakpoint()

        appendTable {
            title = "__**🚛 Transport capacity:**__"
            displayHeaders = false
            topPadding = 1
            column {
                rightPadding = 2
                cells = shippingRate.map(Row::label)
            }
            column {
                alignment = RIGHT
                cells = shippingRate.map(Row::value)
            }
            column {
                leftPadding = 1
                cells = shippingRate.map(Row::suffix)
            }
        }
    }.splitMessage(separator = zwsp)
}
