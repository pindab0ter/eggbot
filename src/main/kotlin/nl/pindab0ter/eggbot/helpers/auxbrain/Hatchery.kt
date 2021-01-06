package nl.pindab0ter.eggbot.helpers.auxbrain

import com.auxbrain.ei.Artifact
import com.auxbrain.ei.Artifact.Name.CHALICE
import com.auxbrain.ei.Artifact.Name.LIFE_STONE
import com.auxbrain.ei.Backup
import nl.pindab0ter.eggbot.helpers.activeSoloArtifactsFor
import nl.pindab0ter.eggbot.helpers.auxbrain.CommonResearch.*
import nl.pindab0ter.eggbot.helpers.auxbrain.EpicResearch.EPIC_INT_HATCHERIES
import nl.pindab0ter.eggbot.helpers.auxbrain.EpicResearch.INTERNAL_HATCH_SHARING
import nl.pindab0ter.eggbot.helpers.productOf
import nl.pindab0ter.eggbot.helpers.sum
import nl.pindab0ter.eggbot.helpers.times
import java.math.BigDecimal
import java.math.BigDecimal.ONE

object Hatchery {
    fun sharingMultiplierFor(backup: Backup): BigDecimal = backup.hatcherySharingMultiplier
    fun multiplierFor(backup: Backup, farm: Backup.Farm): BigDecimal =
        farm.hatcheryRateFlatIncreases.sum()
            .multiply(backup.hatcheryRateMultiplier)
            .multiply(backup.activeSoloArtifactsFor(farm).hatcheryRateMultiplier)

    private val hatcheryRateArtifacts = listOf(
        CHALICE,
        LIFE_STONE,
    )

    private val List<Artifact>.hatcheryRateMultiplier
        get() = productOf { artifact ->
            Artifacts.multiplierFor(artifact, hatcheryRateArtifacts)
        }

    private val Backup.hatcheryRateMultiplier: BigDecimal
        get() = ONE + BigDecimal(".05") * game!!.epicResearch[EPIC_INT_HATCHERIES.ordinal].level
    private val Backup.hatcherySharingMultiplier: BigDecimal
        get() = ONE + BigDecimal(".10") * game!!.epicResearch[INTERNAL_HATCH_SHARING.ordinal].level
    private val Backup.Farm.hatcheryRateFlatIncreases: List<BigDecimal>
        get() = listOf(
            BigDecimal(2 * commonResearch[INTERNAL_HATCHERY1.ordinal].level),
            BigDecimal(5 * commonResearch[INTERNAL_HATCHERY2.ordinal].level),
            BigDecimal(10 * commonResearch[INTERNAL_HATCHERY3.ordinal].level),
            BigDecimal(25 * commonResearch[INTERNAL_HATCHERY4.ordinal].level),
            BigDecimal(5 * commonResearch[MACHINE_LEARNING_INCUBATORS.ordinal].level),
            BigDecimal(50 * commonResearch[NEURAL_LINKING.ordinal].level),
        )
}