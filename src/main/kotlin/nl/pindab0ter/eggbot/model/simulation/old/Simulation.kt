package nl.pindab0ter.eggbot.model.simulation.old

import com.auxbrain.ei.Backup
import com.auxbrain.ei.HabLevel
import com.auxbrain.ei.VehicleType
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.auxbrain.CommonResearch.*
import nl.pindab0ter.eggbot.helpers.auxbrain.EpicResearch.*
import org.joda.time.DateTime
import org.joda.time.Duration
import java.math.BigDecimal

abstract class Simulation(val backup: Backup) {

    internal abstract val farm: Backup.Simulation

    val farmerName: String get() = backup.userName
    val timeSinceLastUpdate: Duration get() = Duration(backup.approxTime.toDateTime(), DateTime.now())

    // region Research

    private val habCapacityMultipliers: List<BigDecimal>
        get() = listOf(
            BigDecimal.ONE + BigDecimal(".05") * farm.commonResearch[HEN_HOUSE_REMODEL.ordinal].level,
            BigDecimal.ONE + BigDecimal(".05") * farm.commonResearch[MICROLUX_CHICKEN_SUITES.ordinal].level,
            BigDecimal.ONE + BigDecimal(".02") * farm.commonResearch[GRAV_PLATING.ordinal].level,
            BigDecimal.ONE + BigDecimal(".02") * farm.commonResearch[WORMHOLE_DAMPENING.ordinal].level
        )

    private val internalHatcheryFlatIncreases: List<BigDecimal>
        get() = listOf(
            BigDecimal(2 * farm.commonResearch[INTERNAL_HATCHERY1.ordinal].level),
            BigDecimal(5 * farm.commonResearch[INTERNAL_HATCHERY2.ordinal].level),
            BigDecimal(10 * farm.commonResearch[INTERNAL_HATCHERY3.ordinal].level),
            BigDecimal(25 * farm.commonResearch[INTERNAL_HATCHERY4.ordinal].level),
            BigDecimal(5 * farm.commonResearch[MACHINE_LEARNING_INCUBATORS.ordinal].level),
            BigDecimal(50 * farm.commonResearch[NEURAL_LINKING.ordinal].level)
        )

    private val internalHatcheryMultiplier: BigDecimal
        get() = BigDecimal.ONE + BigDecimal(".05") * backup.game!!.epicResearch[EPIC_INT_HATCHERIES.ordinal].level

    private val eggLayingMultipliers: List<BigDecimal>
        get() = listOf(
            BigDecimal.ONE + BigDecimal(".10") * farm.commonResearch[COMFORTABLE_NESTS.ordinal].level,
            BigDecimal.ONE + BigDecimal(".05") * farm.commonResearch[HEN_HOUSE_AC.ordinal].level,
            BigDecimal.ONE + BigDecimal(".15") * farm.commonResearch[IMPROVED_GENETICS.ordinal].level,
            BigDecimal.ONE + BigDecimal(".10") * farm.commonResearch[TIME_COMPRESSION.ordinal].level,
            BigDecimal.ONE + BigDecimal(".02") * farm.commonResearch[TIMELINE_DIVERSION.ordinal].level,
            BigDecimal.ONE + BigDecimal(".10") * farm.commonResearch[RELATIVITY_OPTIMIZATION.ordinal].level,
            BigDecimal.ONE + BigDecimal(".05") * backup.game!!.epicResearch[EPIC_COMFY_NESTS.ordinal].level
        )

    private val shippingRatePercentageIncreases: List<BigDecimal>
        get() = listOf(
            BigDecimal.ONE + BigDecimal(".05") * farm.commonResearch[IMPROVED_LEAFSPRINGS.ordinal].level,
            BigDecimal.ONE + BigDecimal(".10") * farm.commonResearch[LIGHTWEIGHT_BOXES.ordinal].level,
            BigDecimal.ONE + BigDecimal(".05") * farm.commonResearch[DRIVER_TRAINING.ordinal].level,
            BigDecimal.ONE + BigDecimal(".05") * farm.commonResearch[SUPER_ALLOY_FRAMES.ordinal].level,
            BigDecimal.ONE + BigDecimal(".05") * farm.commonResearch[QUANTUM_STORAGE.ordinal].level,
            BigDecimal.ONE + BigDecimal(".05") * farm.commonResearch[HOVER_UPGRADES.ordinal].level, // Assumes at least Hover Semi
            BigDecimal.ONE + BigDecimal(".05") * farm.commonResearch[DARK_CONTAINMENT.ordinal].level,
            BigDecimal.ONE + BigDecimal(".05") * farm.commonResearch[NEURAL_NET_REFINEMENT.ordinal].level,
            BigDecimal.ONE + BigDecimal(".05") * farm.commonResearch[HYPER_PORTALLING.ordinal].level,
            BigDecimal.ONE + BigDecimal(".05") * backup.game!!.epicResearch[TRANSPORTATION_LOBBYISTS.ordinal].level
        )

    private val internalHatcheryCalm: BigDecimal
        get() = BigDecimal.ONE + BigDecimal(".10") * backup.game!!.epicResearch[INTERNAL_HATCH_CALM.ordinal].level


    // endregion Research

    // region Habitats (chicken cap)

    private val habsMaxCapacityBonus: BigDecimal by lazy { habCapacityMultipliers.product() }

    private val HabLevel.maxCapacity: BigDecimal get() = capacity.multiply(habsMaxCapacityBonus)

    val habsMaxCapacity: BigDecimal by lazy { farm.habs.sumByBigDecimal { hab -> hab.maxCapacity } }

    // endregion

    // region Internal hatchery (chicken increase)

    private val internalHatcheryRatePerMinute: BigDecimal by lazy {
        (internalHatcheryFlatIncreases.sum() * internalHatcheryMultiplier)
    }

    val populationIncreasePerMinute: BigDecimal by lazy {
        farm.habs
            .foldIndexed(BigDecimal.ZERO) { index, acc, hab ->
                acc + if (hab.maxCapacity < farm.habPopulation[index].toBigDecimal() || hab == HabLevel.NO_HAB) BigDecimal.ZERO else BigDecimal.ONE
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

    private val eggLayingBaseRate: BigDecimal = BigDecimal.ONE / BigDecimal(30)

    val eggsPerChickenPerMinute: BigDecimal by lazy { (eggLayingBaseRate * eggLayingBonus * 60).round(4) }

    // endregion

    // region Shipping rate (max egg laying rate)

    private val shippingRateBonus: BigDecimal by lazy { shippingRatePercentageIncreases.product() }

    val shippingRatePerMinute: BigDecimal by lazy {
        farm.vehicles.foldIndexed(BigDecimal.ZERO) { index, acc, vehicleType ->
            when (vehicleType) {
                VehicleType.HYPERLOOP_TRAIN -> acc + vehicleType.capacity * farm.hyperloopCars[index]
                else -> acc + vehicleType.capacity
            }
        }.multiply(shippingRateBonus)
    }

    // endregion

    // region Boost tokens

    val boostTokensCurrent: Int by lazy { farm.boostTokensReceived - farm.boostTokensGiven - farm.boostTokensSpent }
    val boostTokensSpent: Int get() = farm.boostTokensSpent

    // endregion

    abstract var elapsed: Duration
    abstract val currentEggs: BigDecimal
    abstract var projectedEggs: BigDecimal
    abstract val currentPopulation: BigDecimal
    abstract var projectedPopulation: BigDecimal
    abstract var eggspected: BigDecimal
    val currentEggsPerHour: BigDecimal get() = currentPopulation * eggsPerChickenPerMinute * 60
}