package nl.pindab0ter.eggbot.auxbrain

import com.auxbrain.ei.EggInc
import com.auxbrain.ei.EggInc.Backup
import com.auxbrain.ei.EggInc.FarmType.HOME
import com.auxbrain.ei.EggInc.VehicleType.HYPERLOOP_TRAIN
import nl.pindab0ter.eggbot.*
import nl.pindab0ter.eggbot.auxbrain.CommonResearch.*
import nl.pindab0ter.eggbot.auxbrain.EpicResearch.*
import org.joda.time.Duration
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigDecimal.ZERO
import java.math.MathContext.DECIMAL32
import java.math.RoundingMode.HALF_UP


open class HomeSimulation(val backup: Backup) {

    // Unless stated otherwise, all rates are per second

    private val farm = backup.farmsList.find { it.farmType == HOME }!!

    //
    // Basic info
    //

    val farmerName: String = backup.name

    //
    // Research
    //

    private val internalHatcheryFlatIncreases: List<BigDecimal> = listOf(
        BigDecimal(2 * farm.commonResearchList[INTERNAL_HATCHERY1.ordinal].level),
        BigDecimal(5 * farm.commonResearchList[INTERNAL_HATCHERY2.ordinal].level),
        BigDecimal(10 * farm.commonResearchList[INTERNAL_HATCHERY3.ordinal].level),
        BigDecimal(25 * farm.commonResearchList[INTERNAL_HATCHERY4.ordinal].level),
        BigDecimal(5 * farm.commonResearchList[MACHINE_LEARNING_INCUBATORS.ordinal].level),
        BigDecimal(50 * farm.commonResearchList[NEURAL_LINKING.ordinal].level)
    )

    private val habCapacityMultipliers: List<BigDecimal> = listOf(
        BigDecimal(1 + .05 * farm.commonResearchList[HEN_HOUSE_REMODEL.ordinal].level),
        BigDecimal(1 + .05 * farm.commonResearchList[MICROLUX_CHICKEN_SUITES.ordinal].level),
        BigDecimal(1 + .02 * farm.commonResearchList[GRAV_PLATING.ordinal].level),
        BigDecimal(1 + .02 * farm.commonResearchList[WORMHOLE_DAMPENING.ordinal].level)
    )

    private val internalHatcheryMultiplier: BigDecimal =
        BigDecimal(1 + .05 * backup.data.epicResearchList[EPIC_INT_HATCHERIES.ordinal].level)

    private val eggLayingMultipliers: List<BigDecimal> = listOf(
        BigDecimal(1 + .10 * farm.commonResearchList[COMFORTABLE_NESTS.ordinal].level),
        BigDecimal(1 + .05 * farm.commonResearchList[HEN_HOUSE_AC.ordinal].level),
        BigDecimal(1 + .15 * farm.commonResearchList[IMPROVED_GENETICS.ordinal].level),
        BigDecimal(1 + .10 * farm.commonResearchList[TIME_COMPRESSION.ordinal].level),
        BigDecimal(1 + .02 * farm.commonResearchList[TIMELINE_DIVERSION.ordinal].level),
        BigDecimal(1 + .10 * farm.commonResearchList[RELATIVITY_OPTIMIZATION.ordinal].level),
        BigDecimal(1 + .05 * backup.data.epicResearchList[EPIC_COMFY_NESTS.ordinal].level)
    )

    private val shippingRatePercentageIncreases: List<BigDecimal> = listOf(
        BigDecimal(1 + .05 * farm.commonResearchList[IMPROVED_LEAFSPRINGS.ordinal].level),
        BigDecimal(1 + .10 * farm.commonResearchList[LIGHTWEIGHT_BOXES.ordinal].level),
        BigDecimal(1 + .05 * farm.commonResearchList[DRIVER_TRAINING.ordinal].level),
        BigDecimal(1 + .05 * farm.commonResearchList[SUPER_ALLOY_FRAMES.ordinal].level),
        BigDecimal(1 + .05 * farm.commonResearchList[QUANTUM_STORAGE.ordinal].level),
        BigDecimal(1 + .05 * farm.commonResearchList[HOVER_UPGRADES.ordinal].level), // Assumes at least Hover Semi
        BigDecimal(1 + .05 * farm.commonResearchList[DARK_CONTAINMENT.ordinal].level),
        BigDecimal(1 + .05 * farm.commonResearchList[NEURAL_NET_REFINEMENT.ordinal].level),
        BigDecimal(1 + .05 * backup.data.epicResearchList[TRANSPORTATION_LOBBYISTS.ordinal].level)
    )

    private val internalHatcheryCalm: BigDecimal
        get() = BigDecimal(1 + 0.10 * backup.data.epicResearchList[INTERNAL_HATCH_CALM.ordinal].level)

    //
    // Chickens
    //

    val population: BigDecimal
        get() = farm.numChickens.toBigDecimal()

    //
    // Habitats (chicken cap)
    //

    val habsMaxCapacityBonus: BigDecimal
        get() = habCapacityMultipliers.reduce { acc, bonus -> acc * bonus }

    val EggInc.HabLevel.maxCapacity: BigDecimal
        get() = capacity.multiply(habsMaxCapacityBonus, DECIMAL32)

    val habsMaxCapacity: BigDecimal
        get() = farm.habsList.sumBy { hab -> hab.maxCapacity }

    // Disregards Internal Hatchery Sharing and takes the average of each hab's time to full.
    // TODO: Include Internal Hatchery Sharing and soft knees for each full hab
    val timeToMaxHabs: Duration
        get() = farm.habsList
            .mapIndexed { index, hab -> hab.maxCapacity - farm.habPopulation[index] }
            .map { roomToGrow ->
                roomToGrow
                    .divide(internalHatcheryRate, HALF_UP)
                    .toLong()
                    .toDuration()
            }
            .sum()
            .dividedBy(farm.habsList.foldIndexed(0L) { index, acc, hab ->
                acc + if (hab.capacity.toInt() >= farm.habPopulationList[index]) 0L else 1L
            }.coerceAtLeast(1))

    //
    // Internal hatchery (chicken increase)
    //

    val internalHatcheryRate: BigDecimal
        get() = (internalHatcheryFlatIncreases.sum() * internalHatcheryMultiplier)
            .divide(BigDecimal(60), 8, HALF_UP) // Convert from minutes to seconds


    // TODO: Include Internal Hatchery Sharing for full habs
    val populationIncreaseRate: BigDecimal
        get() = farm.habsList
            .foldIndexed(ZERO) { index, acc, hab -> acc + if (hab.capacity >= farm.habPopulation[index]) ONE else ZERO }
            .times(internalHatcheryRate)

    //
    // Eggs
    //

    val eggsLaid
        get() = farm.eggsLaid.toBigDecimal()

    val eggLayingBaseRate: BigDecimal
        get() = ONE.divide(BigDecimal(30), 64, HALF_UP)

    val eggLayingBonus: BigDecimal
        get() = eggLayingMultipliers.reduce { acc, bonus -> acc.multiply(bonus) }

    val eggLayingRate: BigDecimal
        get() = population * eggLayingBaseRate * eggLayingBonus

    //
    // Shipping rate (max egg laying rate)
    //

    private val shippingRateBonus: BigDecimal
        get() = shippingRatePercentageIncreases.reduce { acc, bonus -> acc.multiply(bonus) }

    val shippingRate: BigDecimal
        get() = farm.vehiclesList.foldIndexed(ZERO) { index, acc, vehicleType ->
            when (vehicleType) {
                HYPERLOOP_TRAIN -> acc + vehicleType.capacity * farm.hyperloopCarsList[index]
                else -> acc + vehicleType.capacity
            }
        }.multiply(shippingRateBonus)

    val currentEggLayingRate
        get() = minOf(eggLayingRate, shippingRate)

    val currentEggLayingRatePerHour
        get() = currentEggLayingRate.multiply(BigDecimal(60 * 60))

    //
    // Bottlenecks
    //

    val timeToMaxShippingRate: Duration
        get() = Duration(((shippingRate / eggLayingRate * population - population) / internalHatcheryRate).toLong())

}
