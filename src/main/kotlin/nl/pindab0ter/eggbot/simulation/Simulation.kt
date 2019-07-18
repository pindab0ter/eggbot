package nl.pindab0ter.eggbot.simulation

import com.auxbrain.ei.EggInc
import com.auxbrain.ei.EggInc.HabLevel.NO_HAB
import nl.pindab0ter.eggbot.*
import org.joda.time.Duration
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigDecimal.ZERO
import java.math.MathContext.DECIMAL64

abstract class Simulation(val backup: EggInc.Backup) {

    internal abstract val farm: EggInc.Simulation

    //
    // Basic info
    //

    val farmerName: String = backup.name

    //
    // Research
    //

    private val internalHatcheryFlatIncreases: List<BigDecimal>
        get() = listOf(
            BigDecimal(2 * farm.commonResearchList[CommonResearch.INTERNAL_HATCHERY1.ordinal].level),
            BigDecimal(5 * farm.commonResearchList[CommonResearch.INTERNAL_HATCHERY2.ordinal].level),
            BigDecimal(10 * farm.commonResearchList[CommonResearch.INTERNAL_HATCHERY3.ordinal].level),
            BigDecimal(25 * farm.commonResearchList[CommonResearch.INTERNAL_HATCHERY4.ordinal].level),
            BigDecimal(5 * farm.commonResearchList[CommonResearch.MACHINE_LEARNING_INCUBATORS.ordinal].level),
            BigDecimal(50 * farm.commonResearchList[CommonResearch.NEURAL_LINKING.ordinal].level)
        )

    private val habCapacityMultipliers: List<BigDecimal>
        get() = listOf(
            BigDecimal(1 + .05 * farm.commonResearchList[CommonResearch.HEN_HOUSE_REMODEL.ordinal].level),
            BigDecimal(1 + .05 * farm.commonResearchList[CommonResearch.MICROLUX_CHICKEN_SUITES.ordinal].level),
            BigDecimal(1 + .02 * farm.commonResearchList[CommonResearch.GRAV_PLATING.ordinal].level),
            BigDecimal(1 + .02 * farm.commonResearchList[CommonResearch.WORMHOLE_DAMPENING.ordinal].level)
        )

    private val internalHatcheryMultiplier: BigDecimal
        get() = BigDecimal(1 + .05 * backup.data.epicResearchList[EpicResearch.EPIC_INT_HATCHERIES.ordinal].level)

    private val eggLayingMultipliers: List<BigDecimal>
        get() = listOf(
            BigDecimal(1 + .10 * farm.commonResearchList[CommonResearch.COMFORTABLE_NESTS.ordinal].level),
            BigDecimal(1 + .05 * farm.commonResearchList[CommonResearch.HEN_HOUSE_AC.ordinal].level),
            BigDecimal(1 + .15 * farm.commonResearchList[CommonResearch.IMPROVED_GENETICS.ordinal].level),
            BigDecimal(1 + .10 * farm.commonResearchList[CommonResearch.TIME_COMPRESSION.ordinal].level),
            BigDecimal(1 + .02 * farm.commonResearchList[CommonResearch.TIMELINE_DIVERSION.ordinal].level),
            BigDecimal(1 + .10 * farm.commonResearchList[CommonResearch.RELATIVITY_OPTIMIZATION.ordinal].level),
            BigDecimal(1 + .05 * backup.data.epicResearchList[EpicResearch.EPIC_COMFY_NESTS.ordinal].level)
        )

    private val shippingRatePercentageIncreases: List<BigDecimal>
        get() = listOf(
            BigDecimal(1 + .05 * farm.commonResearchList[CommonResearch.IMPROVED_LEAFSPRINGS.ordinal].level),
            BigDecimal(1 + .10 * farm.commonResearchList[CommonResearch.LIGHTWEIGHT_BOXES.ordinal].level),
            BigDecimal(1 + .05 * farm.commonResearchList[CommonResearch.DRIVER_TRAINING.ordinal].level),
            BigDecimal(1 + .05 * farm.commonResearchList[CommonResearch.SUPER_ALLOY_FRAMES.ordinal].level),
            BigDecimal(1 + .05 * farm.commonResearchList[CommonResearch.QUANTUM_STORAGE.ordinal].level),
            BigDecimal(1 + .05 * farm.commonResearchList[CommonResearch.HOVER_UPGRADES.ordinal].level), // Assumes at least Hover Semi
            BigDecimal(1 + .05 * farm.commonResearchList[CommonResearch.DARK_CONTAINMENT.ordinal].level),
            BigDecimal(1 + .05 * farm.commonResearchList[CommonResearch.NEURAL_NET_REFINEMENT.ordinal].level),
            BigDecimal(1 + .05 * backup.data.epicResearchList[EpicResearch.TRANSPORTATION_LOBBYISTS.ordinal].level)
        )

    private val internalHatcheryCalm: BigDecimal
        get() = BigDecimal(1 + .10 * backup.data.epicResearchList[EpicResearch.INTERNAL_HATCH_CALM.ordinal].level)

    //
    // Habitats (chicken cap)
    //

    private val habsMaxCapacityBonus: BigDecimal by lazy { habCapacityMultipliers.product() }

    val EggInc.HabLevel.maxCapacity: BigDecimal get() = capacity.multiply(habsMaxCapacityBonus)

    val habsMaxCapacity: BigDecimal by lazy { farm.habsList.sumBy { hab -> hab.maxCapacity } }

    val timeToFullHabs: Duration by lazy {
        if (internalHatcheriesAreActive) {
            val remainingCapacity = habsMaxCapacity - population
            val secondsToFullHabs = remainingCapacity / populationIncreaseRatePerSecond
            secondsToFullHabs.toLong().toDuration()
        } else Duration(Long.MAX_VALUE)
    }

    //
    // Internal hatchery (chicken increase)
    //

    val internalHatcheryRatePerMinute: BigDecimal by lazy {
        (internalHatcheryFlatIncreases.sum() * internalHatcheryMultiplier)
    }

    val internalHatcheryRatePerSecond: BigDecimal by lazy {
        internalHatcheryRatePerMinute / BigDecimal(60)
    }

    val internalHatcheriesAreActive: Boolean by lazy {
        internalHatcheryRatePerMinute > ZERO
    }

    // TODO: Include Internal Hatchery Sharing for full habs
    val populationIncreaseRatePerMinute: BigDecimal by lazy {
        farm.habsList
            .foldIndexed(ZERO) { index, acc, hab ->
                acc + if (hab.maxCapacity < farm.habPopulation[index] || hab == NO_HAB) ZERO else ONE
            }
            .times(internalHatcheryRatePerMinute)
            .times(internalHatcheryCalm)
    }

    val populationIncreaseRatePerSecond: BigDecimal by lazy {
        populationIncreaseRatePerMinute / BigDecimal(60)
    }

    val populationIncreaseRatePerHour: BigDecimal by lazy {
        populationIncreaseRatePerMinute * BigDecimal(60)
    }

    //
    // Chickens
    //

    lateinit var population: BigDecimal

    //
    // Eggs
    //

    private val eggLayingBonus: BigDecimal by lazy { eggLayingMultipliers.product() }

    lateinit var eggsLaid: BigDecimal

    val eggLayingBaseRatePerChickenPerSecond: BigDecimal by lazy { ONE / BigDecimal(30) }

    val eggLayingRatePerChickenPerSecond: BigDecimal by lazy { eggLayingBaseRatePerChickenPerSecond * eggLayingBonus }

    val eggLayingRatePerSecond: BigDecimal by lazy { eggLayingRatePerChickenPerSecond * population }

    val eggLayingRatePerMinute: BigDecimal by lazy { eggLayingRatePerSecond * 60 }

    val eggLayingRatePerHour: BigDecimal by lazy { eggLayingRatePerSecond * 60 * 60 }

    //
    // Shipping rate (max egg laying rate)
    //

    private val shippingRateBonus: BigDecimal by lazy { shippingRatePercentageIncreases.product() }

    val shippingRatePerMinute: BigDecimal by lazy {
        farm.vehiclesList.foldIndexed(ZERO) { index, acc, vehicleType ->
            when (vehicleType) {
                EggInc.VehicleType.HYPERLOOP_TRAIN -> acc + vehicleType.capacity * farm.hyperloopCarsList[index]
                else -> acc + vehicleType.capacity
            }
        }.multiply(shippingRateBonus)
    }

    val shippingRatePerSecond: BigDecimal by lazy {
        shippingRatePerMinute.divide(BigDecimal(60), DECIMAL64)
    }

    val timeToMaxShippingRate: Duration by lazy {
        if (internalHatcheriesAreActive) {
            val shippingRateRemaining = shippingRatePerSecond - currentEggLayingRatePerSecond
            val chickensRequired = shippingRateRemaining.divide(eggLayingRatePerChickenPerSecond, DECIMAL64)
            val secondsToMaxShipping = chickensRequired.divide(populationIncreaseRatePerSecond, DECIMAL64)
            secondsToMaxShipping.toLong().toDuration()
        } else Duration(Long.MAX_VALUE)
    }

    val currentEggLayingRatePerSecond by lazy { minOf(eggLayingRatePerSecond, shippingRatePerSecond) }

    val currentEggLayingRatePerMinute by lazy { minOf(eggLayingRatePerMinute, shippingRatePerMinute) }

    val currentEggLayingRatePerHour by lazy { currentEggLayingRatePerMinute * 60 }
}