package nl.pindab0ter.eggbot.helpers.auxbrain

import com.auxbrain.ei.Artifact
import com.auxbrain.ei.Artifact.Name.*
import com.auxbrain.ei.Backup
import nl.pindab0ter.eggbot.helpers.activeSoloArtifactsFor
import nl.pindab0ter.eggbot.helpers.auxbrain.CommonResearch.*
import nl.pindab0ter.eggbot.helpers.product
import nl.pindab0ter.eggbot.helpers.productOf
import nl.pindab0ter.eggbot.helpers.times
import java.math.BigDecimal
import java.math.BigDecimal.ONE

object EggLayingRate {
    fun multiplierFor(farm: Backup.Farm, backup: Backup, activeCoopArtifacts: List<Artifact>): BigDecimal =
        farm.eggLayingCommonResearchMultipliers
            .plus(backup.eggLayingEpicResearchMultiplier)
            .product()
            .multiply(backup.activeSoloArtifactsFor(farm).eggLayingRateMultiplier)
            .multiply(activeCoopArtifacts.eggLayingRateMultiplier)

    private val eggLayingRateArtifacts = listOf(
        QUANTUM_METRONOME,
        TACHYON_STONE,
    )

    val coopEggLayingRateArtifacts = listOf(
        TACHYON_DEFLECTOR,
    )

    private val Backup.eggLayingEpicResearchMultiplier: BigDecimal
        get() = ONE + BigDecimal(".05") * game!!.epicResearch[EpicResearch.EPIC_COMFY_NESTS.ordinal].level

    private val List<Artifact>.eggLayingRateMultiplier
        get() = productOf { artifact ->
            Artifacts.multiplierFor(artifact, eggLayingRateArtifacts)
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
}
