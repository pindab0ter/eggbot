package nl.pindab0ter.eggbot.model.auxbrain

import com.auxbrain.ei.Artifact
import com.auxbrain.ei.Artifact.Name.*
import com.auxbrain.ei.Backup
import com.auxbrain.ei.CoopStatus
import nl.pindab0ter.eggbot.helpers.product
import nl.pindab0ter.eggbot.helpers.productOf
import nl.pindab0ter.eggbot.helpers.times
import nl.pindab0ter.eggbot.model.auxbrain.CommonResearch.*
import java.math.BigDecimal
import java.math.BigDecimal.ONE

private fun Backup.eggLayingRateResearchMultiplierFor(farm: Backup.Farm) = farm.eggLayingRateCommonResearchMultipliers
    .plus(eggLayingRateEpicResearchMultiplier)
    .product()

private val CoopStatus.FarmInfo.eggLayingRateResearchMultiplier: BigDecimal
    get() = eggLayingRateCommonResearchMultipliers
        .plus(eggLayingRateEpicResearchMultiplier)
        .product()

private fun Backup.eggLayingRateArtifactsMultiplierFor(
    farm: Backup.Farm,
    coopArtifacts: List<Artifact> = emptyList(),
): BigDecimal {
    val farmArtifacts = artifactsFor(farm)
    val nonCoopArtifacts = farmArtifacts.filter { artifact -> artifact.name != TACHYON_DEFLECTOR }
    val remainingCoopArtifacts = farmArtifacts.fold(coopArtifacts) { acc, artifact -> acc.minusElement(artifact) }

    return nonCoopArtifacts.eggLayingRateMultiplier
        .multiply(remainingCoopArtifacts.eggLayingRateMultiplier)
}

fun Backup.eggLayingRateMultiplierFor(
    farm: Backup.Farm,
    coopArtifacts: List<Artifact> = emptyList(),
): BigDecimal = eggLayingRateResearchMultiplierFor(farm)
    .multiply(eggLayingRateArtifactsMultiplierFor(farm, coopArtifacts))

val CoopStatus.FarmInfo.eggLayingRateMultiplier: BigDecimal
    get() = eggLayingRateResearchMultiplier
        .multiply(artifacts.eggLayingRateMultiplier)

val eggLayingRateArtifacts = listOf(
    QUANTUM_METRONOME,
    TACHYON_STONE,
    TACHYON_DEFLECTOR,
)

private val Backup.eggLayingRateEpicResearchMultiplier: BigDecimal
    get() = ONE + BigDecimal(".05") * game.epicResearch[EpicResearch.EPIC_COMFY_NESTS.ordinal].level

private val CoopStatus.FarmInfo.eggLayingRateEpicResearchMultiplier: BigDecimal
    get() = ONE + BigDecimal(".05") * epicResearch[EpicResearch.EPIC_COMFY_NESTS.ordinal].level

private val List<Artifact>.eggLayingRateMultiplier
    get() = filter { artifact ->
        artifact.name in eggLayingRateArtifacts
    }.productOf { artifact ->
        artifact.multiplier
    }

private val Backup.Farm.eggLayingRateCommonResearchMultipliers: List<BigDecimal>
    get() = listOf(
        ONE + BigDecimal(".10") * commonResearch[COMFORTABLE_NESTS.ordinal].level,
        ONE + BigDecimal(".05") * commonResearch[HEN_HOUSE_AC.ordinal].level,
        ONE + BigDecimal(".15") * commonResearch[IMPROVED_GENETICS.ordinal].level,
        ONE + BigDecimal(".10") * commonResearch[TIME_COMPRESSION.ordinal].level,
        ONE + BigDecimal(".02") * commonResearch[TIMELINE_DIVERSION.ordinal].level,
        ONE + BigDecimal(".10") * commonResearch[RELATIVITY_OPTIMIZATION.ordinal].level,
    )

private val CoopStatus.FarmInfo.eggLayingRateCommonResearchMultipliers: List<BigDecimal>
    get() = listOf(
        ONE + BigDecimal(".10") * commonResearch[COMFORTABLE_NESTS.ordinal].level,
        ONE + BigDecimal(".05") * commonResearch[HEN_HOUSE_AC.ordinal].level,
        ONE + BigDecimal(".15") * commonResearch[IMPROVED_GENETICS.ordinal].level,
        ONE + BigDecimal(".10") * commonResearch[TIME_COMPRESSION.ordinal].level,
        ONE + BigDecimal(".02") * commonResearch[TIMELINE_DIVERSION.ordinal].level,
        ONE + BigDecimal(".10") * commonResearch[RELATIVITY_OPTIMIZATION.ordinal].level,
    )
