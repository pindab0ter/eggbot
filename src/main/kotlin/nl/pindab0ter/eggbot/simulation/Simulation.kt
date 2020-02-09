package nl.pindab0ter.eggbot.simulation

import com.auxbrain.ei.EggInc.*
import com.auxbrain.ei.EggInc.HabLevel.NO_HAB
import nl.pindab0ter.eggbot.simulation.CommonResearch.*
import nl.pindab0ter.eggbot.simulation.EpicResearch.*
import nl.pindab0ter.eggbot.utilities.*
import org.joda.time.DateTime
import org.joda.time.Duration
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigDecimal.ZERO

abstract class Simulation(val backup: Backup) {

    internal abstract val farm: Backup.Simulation

    val farmerName: String get() = backup.userName
    val timeSinceLastUpdate: Duration get() = Duration(backup.approxTime.toDateTime(), DateTime.now())

    // region Research

    private val habCapacityMultipliers: List<BigDecimal>
        get() = listOf(
            ONE + BigDecimal(".05") * farm.commonResearchList[HEN_HOUSE_REMODEL.ordinal].level,
            ONE + BigDecimal(".05") * farm.commonResearchList[MICROLUX_CHICKEN_SUITES.ordinal].level,
            ONE + BigDecimal(".02") * farm.commonResearchList[GRAV_PLATING.ordinal].level,
            ONE + BigDecimal(".02") * farm.commonResearchList[WORMHOLE_DAMPENING.ordinal].level
        )

    private val internalHatcheryFlatIncreases: List<BigDecimal>
        get() = listOf(
            BigDecimal(2 * farm.commonResearchList[INTERNAL_HATCHERY1.ordinal].level),
            BigDecimal(5 * farm.commonResearchList[INTERNAL_HATCHERY2.ordinal].level),
            BigDecimal(10 * farm.commonResearchList[INTERNAL_HATCHERY3.ordinal].level),
            BigDecimal(25 * farm.commonResearchList[INTERNAL_HATCHERY4.ordinal].level),
            BigDecimal(5 * farm.commonResearchList[MACHINE_LEARNING_INCUBATORS.ordinal].level),
            BigDecimal(50 * farm.commonResearchList[NEURAL_LINKING.ordinal].level)
        )

    private val internalHatcheryMultiplier: BigDecimal
        get() = ONE + BigDecimal(".05") * backup.game.epicResearchList[EPIC_INT_HATCHERIES.ordinal].level

    private val eggLayingMultipliers: List<BigDecimal>
        get() = listOf(
            ONE + BigDecimal(".10") * farm.commonResearchList[COMFORTABLE_NESTS.ordinal].level,
            ONE + BigDecimal(".05") * farm.commonResearchList[HEN_HOUSE_AC.ordinal].level,
            ONE + BigDecimal(".15") * farm.commonResearchList[IMPROVED_GENETICS.ordinal].level,
            ONE + BigDecimal(".10") * farm.commonResearchList[TIME_COMPRESSION.ordinal].level,
            ONE + BigDecimal(".02") * farm.commonResearchList[TIMELINE_DIVERSION.ordinal].level,
            ONE + BigDecimal(".10") * farm.commonResearchList[RELATIVITY_OPTIMIZATION.ordinal].level,
            ONE + BigDecimal(".05") * backup.game.epicResearchList[EPIC_COMFY_NESTS.ordinal].level
        )

    private val shippingRatePercentageIncreases: List<BigDecimal>
        get() = listOf(
            ONE + BigDecimal(".05") * farm.commonResearchList[IMPROVED_LEAFSPRINGS.ordinal].level,
            ONE + BigDecimal(".10") * farm.commonResearchList[LIGHTWEIGHT_BOXES.ordinal].level,
            ONE + BigDecimal(".05") * farm.commonResearchList[DRIVER_TRAINING.ordinal].level,
            ONE + BigDecimal(".05") * farm.commonResearchList[SUPER_ALLOY_FRAMES.ordinal].level,
            ONE + BigDecimal(".05") * farm.commonResearchList[QUANTUM_STORAGE.ordinal].level,
            ONE + BigDecimal(".05") * farm.commonResearchList[HOVER_UPGRADES.ordinal].level, // Assumes at least Hover Semi
            ONE + BigDecimal(".05") * farm.commonResearchList[DARK_CONTAINMENT.ordinal].level,
            ONE + BigDecimal(".05") * farm.commonResearchList[NEURAL_NET_REFINEMENT.ordinal].level,
            ONE + BigDecimal(".05") * farm.commonResearchList[HYPER_PORTALLING.ordinal].level,
            ONE + BigDecimal(".05") * backup.game.epicResearchList[TRANSPORTATION_LOBBYISTS.ordinal].level
        )

    private val internalHatcheryCalm: BigDecimal
        get() = ONE + BigDecimal(".10") * backup.game.epicResearchList[INTERNAL_HATCH_CALM.ordinal].level


    // endregion Research

    // region Habitats (chicken cap)

    private val habsMaxCapacityBonus: BigDecimal by lazy { habCapacityMultipliers.product() }

    private val HabLevel.maxCapacity: BigDecimal get() = capacity.multiply(habsMaxCapacityBonus)

    val habsMaxCapacity: BigDecimal by lazy { farm.habsList.sumByBigDecimal { hab -> hab.maxCapacity } }

    // endregion

    // region Internal hatchery (chicken increase)

    private val internalHatcheryRatePerMinute: BigDecimal by lazy {
        (internalHatcheryFlatIncreases.sum() * internalHatcheryMultiplier)
    }

    // TODO: Include Internal Hatchery Sharing for full habs
    val populationIncreasePerMinute: BigDecimal by lazy {
        farm.habsList
            .foldIndexed(ZERO) { index, acc, hab ->
                acc + if (hab.maxCapacity < farm.habPopulation[index] || hab == NO_HAB) ZERO else ONE
            }
            .times(internalHatcheryRatePerMinute)
            .times(internalHatcheryCalm)
    }

    val populationIncreasePerHour: BigDecimal by lazy {
        populationIncreasePerMinute * BigDecimal(60)
    }

    // endregion

    // region Egg laying rates

    private val eggLayingBonus: BigDecimal by lazy { eggLayingMultipliers.product() }

    private val eggLayingBaseRate: BigDecimal = ONE / BigDecimal(30)

    val eggsPerChickenPerMinute: BigDecimal by lazy { (eggLayingBaseRate * eggLayingBonus * 60).round(4) }

    // endregion

    // region Shipping rate (max egg laying rate)

    private val shippingRateBonus: BigDecimal by lazy { shippingRatePercentageIncreases.product() }

    val shippingRatePerMinute: BigDecimal by lazy {
        farm.vehiclesList.foldIndexed(ZERO) { index, acc, vehicleType ->
            when (vehicleType) {
                VehicleType.HYPERLOOP_TRAIN -> acc + vehicleType.capacity * farm.hyperloopCarsList[index]
                else -> acc + vehicleType.capacity
            }
        }.multiply(shippingRateBonus)
    }

    // endregion

    // region Boost tokens

    val boostTokensCurrent: Int by lazy { farm.boostTokensReceived - farm.boostTokensGiven - farm.boostTokensSpent }

    // endregion

    abstract var elapsed: Duration
    abstract val currentEggs: BigDecimal
    abstract var projectedEggs: BigDecimal
    abstract val currentPopulation: BigDecimal
    abstract var projectedPopulation: BigDecimal
    abstract var eggspected: BigDecimal
    val currentEggsPerHour: BigDecimal get() = currentPopulation * eggsPerChickenPerMinute * 60

}