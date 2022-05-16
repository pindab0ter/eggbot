package nl.pindab0ter.eggbot.model.auxbrain

import com.auxbrain.ei.Artifact
import com.auxbrain.ei.Artifact.Name.CHALICE
import com.auxbrain.ei.Artifact.Name.LIFE_STONE
import com.auxbrain.ei.Backup
import com.auxbrain.ei.Backup.Farm
import nl.pindab0ter.eggbot.helpers.productOf
import nl.pindab0ter.eggbot.helpers.sum
import nl.pindab0ter.eggbot.helpers.times
import nl.pindab0ter.eggbot.model.auxbrain.CommonResearch.*
import nl.pindab0ter.eggbot.model.auxbrain.EpicResearch.EPIC_INT_HATCHERIES
import nl.pindab0ter.eggbot.model.auxbrain.EpicResearch.INTERNAL_HATCH_SHARING
import java.math.BigDecimal
import java.math.BigDecimal.ONE

fun Backup.hatcheryRateFromResearchFor(farm: Farm): BigDecimal = farm
    .hatcheryRateFlatIncreases.sum()
    .multiply(hatcheryRateMultiplier)

fun Backup.hatcheryRateArtifactsMultiplierFor(farm: Farm): BigDecimal =
    artifactsFor(farm).hatcheryRateMultiplier

fun Backup.hatcheryRateFor(farm: Farm): BigDecimal = hatcheryRateFromResearchFor(farm)
    .multiply(hatcheryRateArtifactsMultiplierFor(farm))

val hatcheryRateArtifacts = listOf(
    CHALICE,
    LIFE_STONE,
)

private val List<Artifact>.hatcheryRateMultiplier
    get() = filter { artifact ->
        artifact.name in hatcheryRateArtifacts
    }.productOf { artifact ->
        artifact.multiplier
    }

private val Backup.hatcheryRateMultiplier: BigDecimal
    get() = ONE + BigDecimal(".05") * game!!.epicResearch[EPIC_INT_HATCHERIES.ordinal].level
val Backup.hatcherySharingMultiplier: BigDecimal
    get() = ONE + BigDecimal(".10") * game!!.epicResearch[INTERNAL_HATCH_SHARING.ordinal].level
private val Farm.hatcheryRateFlatIncreases: List<BigDecimal>
    get() = listOf(
        BigDecimal(2 * commonResearch[INTERNAL_HATCHERY1.ordinal].level),
        BigDecimal(5 * commonResearch[INTERNAL_HATCHERY2.ordinal].level),
        BigDecimal(10 * commonResearch[INTERNAL_HATCHERY3.ordinal].level),
        BigDecimal(25 * commonResearch[INTERNAL_HATCHERY4.ordinal].level),
        BigDecimal(5 * commonResearch[MACHINE_LEARNING_INCUBATORS.ordinal].level),
        BigDecimal(50 * commonResearch[NEURAL_LINKING.ordinal].level),
    )
