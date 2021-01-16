package nl.pindab0ter.eggbot.model.auxbrain

import com.auxbrain.ei.Artifact
import com.auxbrain.ei.Artifact.Name.*
import com.auxbrain.ei.Backup
import nl.pindab0ter.eggbot.helpers.product
import nl.pindab0ter.eggbot.helpers.productOf
import nl.pindab0ter.eggbot.helpers.times
import nl.pindab0ter.eggbot.model.auxbrain.CommonResearch.*
import java.math.BigDecimal
import java.math.BigDecimal.ONE

fun Backup.eggLayingRateResearchMultiplierFor(farm: Backup.Farm) = farm.eggLayingCommonResearchMultipliers
    .plus(eggLayingEpicResearchMultiplier)
    .product()

fun Backup.eggLayingRateArtifactsMultiplierFor(
    farm: Backup.Farm,
    coopArtifacts: List<Artifact> = emptyList(),
): BigDecimal =
    artifactsFor(farm).eggLayingRateMultiplier.multiply(coopArtifacts.minus(artifactsFor(farm)).eggLayingRateMultiplier)

fun Backup.eggLayingRateMultiplierFor(
    farm: Backup.Farm,
    coopArtifacts: List<Artifact> = emptyList(),
): BigDecimal = eggLayingRateResearchMultiplierFor(farm)
    .multiply(artifactsFor(farm).eggLayingRateMultiplier)
    .multiply(coopArtifacts.minus(artifactsFor(farm)).eggLayingRateMultiplier)

val eggLayingRateArtifacts = listOf(
    QUANTUM_METRONOME,
    TACHYON_STONE,
    TACHYON_DEFLECTOR,
)

private val Backup.eggLayingEpicResearchMultiplier: BigDecimal
    get() = ONE + BigDecimal(".05") * game!!.epicResearch[EpicResearch.EPIC_COMFY_NESTS.ordinal].level

private val List<Artifact>.eggLayingRateMultiplier
    get() = filter { artifact ->
        artifact.name in eggLayingRateArtifacts
    }.productOf { artifact ->
        artifact.multiplier
    }

private val Backup.Farm.eggLayingCommonResearchMultipliers: List<BigDecimal>
    get() = listOf(
        ONE + BigDecimal(".10") * commonResearch[COMFORTABLE_NESTS.ordinal].level,
        ONE + BigDecimal(".05") * commonResearch[HEN_HOUSE_AC.ordinal].level,
        ONE + BigDecimal(".15") * commonResearch[IMPROVED_GENETICS.ordinal].level,
        ONE + BigDecimal(".10") * commonResearch[TIME_COMPRESSION.ordinal].level,
        ONE + BigDecimal(".02") * commonResearch[TIMELINE_DIVERSION.ordinal].level,
        ONE + BigDecimal(".10") * commonResearch[RELATIVITY_OPTIMIZATION.ordinal].level,
    )
