package nl.pindab0ter.eggbot

import com.auxbrain.ei.EggInc
import nl.pindab0ter.eggbot.CommonResearch.*
import org.joda.time.DateTime
import org.joda.time.Duration
import java.math.BigDecimal
import java.math.MathContext.DECIMAL128
import kotlin.collections.sumBy

class Simulation(val backup: EggInc.Backup, contractId: String) {
    private val farm = backup.farmsList.find { it.contractId == contractId }!!
    private val localContract = backup.contracts.contractsList.find { it.contract.identifier == contractId }!!

    val eggLayingBonus = BigDecimal(listOf(
        1 + .10 * farm.commonResearchList[COMFORTABLE_NESTS.ordinal].level,
        1 + .05 * farm.commonResearchList[HEN_HOUSE_AC.ordinal].level,
        1 + .15 * farm.commonResearchList[IMPROVED_GENETICS.ordinal].level,
        1 + .10 * farm.commonResearchList[TIME_COMPRESSION.ordinal].level,
        1 + .02 * farm.commonResearchList[TIMELINE_DIVERSION.ordinal].level,
        1 + .05 * backup.data.epicResearchList[EpicResearch.EPIC_COMFY_NESTS.ordinal].level
    ).reduce { acc, bonus -> acc * bonus })

    val internalHatcheryRateMinute = BigDecimal(
        listOf(
            2 * farm.commonResearchList[INTERNAL_HATCHERY1.ordinal].level,
            5 * farm.commonResearchList[INTERNAL_HATCHERY2.ordinal].level,
            10 * farm.commonResearchList[INTERNAL_HATCHERY3.ordinal].level,
            25 * farm.commonResearchList[INTERNAL_HATCHERY4.ordinal].level,
            5 * farm.commonResearchList[MACHINE_LEARNING_INCUBATORS.ordinal].level,
            50 * farm.commonResearchList[NEURAL_LINKING.ordinal].level
        ).sum().times(1 + .05 * backup.data.epicResearchList[EpicResearch.EPIC_INT_HATCHERIES.ordinal].level).times(4) // Assumes four habitats
    )

    val eggLayingRateMinute =
        BigDecimal(farm.numChickens) * eggLayingBonus.divide(BigDecimal(30), DECIMAL128)

    val shippingRateBonus = BigDecimal(listOf(
        1 + .05 * farm.commonResearchList[IMPROVED_LEAFSPRINGS.ordinal].level,
        1 + .10 * farm.commonResearchList[LIGHTWEIGHT_BOXES.ordinal].level,
        1 + .05 * farm.commonResearchList[DRIVER_TRAINING.ordinal].level,
        1 + .05 * farm.commonResearchList[SUPER_ALLOY_FRAMES.ordinal].level,
        1 + .05 * farm.commonResearchList[QUANTUM_STORAGE.ordinal].level,
        1 + .05 * farm.commonResearchList[HOVER_UPGRADES.ordinal].level,
        1 + .05 * farm.commonResearchList[DARK_CONTAINMENT.ordinal].level,
        1 + .05 * farm.commonResearchList[NEURAL_NET_REFINEMENT.ordinal].level,
        1 + .05 * backup.data.epicResearchList[EpicResearch.TRANSPORTATION_LOBBYISTS.ordinal].level
    ).reduce { acc, bonus -> acc * bonus })

    val shippingRate = farm.vehiclesList
        .sumBy { it.capacity }
        .let { BigDecimal(it) }
        .times(shippingRateBonus)

    val effectiveEggLayingRateMinute = minOf(eggLayingRateMinute, shippingRate)

    val elapsedTime = Duration(localContract.timeAccepted.toDateTime(), DateTime.now())
    val secondsRemaining = localContract.contract.lengthSeconds.toDuration().minus(elapsedTime).standardSeconds
    val requiredEggs = localContract.contract
        .goalsList[localContract.contract.goalsList.size - 1]
        .targetAmount

    val `hatchery` = internalHatcheryRateMinute.divide(BigDecimal(60 * 30), DECIMAL128) // 1/3rd per second?

    val finalTarget = BigDecimal(farm.eggsLaid) +
            (eggLayingRateMinute * BigDecimal(secondsRemaining)) +
            (BigDecimal(0.5) * (`hatchery` * eggLayingBonus)) *
            BigDecimal(secondsRemaining) *
            BigDecimal(secondsRemaining) *
            BigDecimal(1 + 0.10 * backup.data.epicResearchList[EpicResearch.INTERNAL_HATCH_CALM.ordinal].level)
}


// @formatter:off
val EggInc.VehicleType.capacity get() = when (this){
    EggInc.VehicleType.UNRECOGNIZED         -> 0
    EggInc.VehicleType.TRIKE                -> 5000
    EggInc.VehicleType.TRANSIT              -> 15000
    EggInc.VehicleType.PICKUP               -> 50000
    EggInc.VehicleType.VEHICLE_10_FOOT      -> 100000
    EggInc.VehicleType.VEHICLE_24_FOOT      -> 250000
    EggInc.VehicleType.SEMI                 -> 500000
    EggInc.VehicleType.DOUBLE_SEMI          -> 1000000
    EggInc.VehicleType.FUTURE_SEMI          -> 5000000
    EggInc.VehicleType.MEGA_SEMI            -> 15000000
    EggInc.VehicleType.HOVER_SEMI           -> 30000000
    EggInc.VehicleType.QUANTUM_TRANSPORTER  -> 50000000
}
// @formatter:on

enum class EpicResearch(val id: String) {
    HOLD_TO_HATCH("hold_to_hatch"),
    EPIC_HATCHERY("epic_hatchery"),
    EPIC_INT_HATCHERIES("epic_internal_incubators"),
    VIDEO_DOUBLER_TIME("video_doubler_time"),
    EPIC_CLUCKING("epic_clucking"),
    EPIC_MULTIPLIER("epic_multiplier"),
    CHEAPER_CONTRACTORS("cheaper_contractors"),
    BUST_UNIONS("bust_unions"),
    LAB_UPGRADE("cheaper_research"),
    SILO_QUALITY("epic_silo_quality"),
    SILO_CAPACITY("silo_capacity"),
    INTERNAL_HATCH_SHARING("int_hatch_sharing"),
    INTERNAL_HATCH_CALM("int_hatch_calm"),
    ACCOUNTING_TRICKS("accounting_tricks"),
    SOUL_FOOD("soul_eggs"),
    PRESTIGE_BONUS("prestige_bonus"),
    DRONE_REWARDS("drone_rewards"),
    EPIC_COMFY_NESTS("epic_egg_laying"),
    TRANSPORTATION_LOBBYISTS("transportation_lobb"),
    WARP_SHIFT("warp_shift"),
    PROPHECY_BONUS("prophecy_bonus"),
    HOLD_TO_RESEARCH("hold_to_research")
}

enum class CommonResearch(val id: String) {
    // Tier 1
    COMFORTABLE_NESTS("comfy_nests"),
    NUTRITIONAL_SUPPLEMENTS("nutritional_sup"),
    BETTER_INCUBATORS("better_incubators"),
    EXCITABLE_CHICKENS("excitable_chickens"),

    // Tier 2
    HEN_HOUSE_REMODEL("hab_capacity1"),
    INTERNAL_HATCHERY1("internal_hatchery1"),
    PADDED_PACKAGING("padded_packaging"),
    HATCHERY_EXPANSION("hatchery_expansion"),
    BIGGER_EGGS("bigger_eggs"),

    // Tier 3
    INTERNAL_HATCHERY2("internal_hatchery2"),
    IMPROVED_LEAFSPRINGS("leafsprings"),
    VEHICLE_RELIABILITY("vehicle_reliablity"),
    ROOSTER_BOOSTER("rooster_booster"),
    COORDINATED_CLUCKING("coordinated_clucking"),

    // Tier 4
    HATCHERY_REBUILD1("hatchery_rebuild1"),
    USDE_PRIME_CERTIFICATION("usde_prime"),
    HEN_HOUSE_AC("hen_house_ac"),
    SUPERFEED_DIET("superfeed"),
    MICROLUX_CHICKEN_SUITES("microlux"),

    // Tier 5
    COMPACT_INCUBATORS("compact_incubators"),
    LIGHTWEIGHT_BOXES("lightweight_boxes"),
    DEPOT_WORKER_EXOSKELETONS("excoskeletons"),
    INTERNAL_HATCHERY3("internal_hatchery3"),
    IMPROVED_GENETICS("improved_genetics"),

    // Tier 6
    TRAFFIC_MANAGEMENT("traffic_management"),
    MOTIVATIONAL_CLUCKING("motivational_clucking"),
    DRIVER_TRAINING("driver_training"),
    SHELL_FORTIFICATION("shell_fortification"),

    // Tier 7
    EGG_LOADING_BOTS("egg_loading_bots"),
    SUPER_ALLOY_FRAMES("super_alloy"),
    EVEN_BIGGER_EGGS("even_bigger_eggs"),
    INTERNAL_HATCHERY4("internal_hatchery4"),

    // Tier 8
    QUANTUM_STORAGE("quantum_storage"),
    GENETIC_PURIFICATION("genetic_purification"),
    MACHINE_LEARNING_INCUBATORS("internal_hatchery5"),
    TIME_COMPRESSION("time_compress"),

    // Tier 9
    HOVER_UPGRADES("hover_upgrades"),
    GRAVITON_COATING("graviton_coating"),
    GRAV_PLATING("grav_plating"),
    CHRYSTALLINE_SHELLING("chrystal_shells"),

    // Tier 10
    AUTONOMOUS_VEHICLES("autonomous_vehicles"),
    NEURAL_LINKING("neural_linking"),
    TELEPATHIC_WILL("telepathic_will"),
    ENLIGHTENED_CHICKENS("enlightened_chickens"),

    // Tier 11
    DARK_CONTAINMENT("dark_containment"),
    ATOMIC_PURIFICATION("atomic_purification"),
    MULTIVERSAL_LAYERING("multi_layering"),
    TIMELINE_DIVERSION("timeline_diversion"),

    // Tier 12
    WORMHOLE_DAMPENING("wormhole_dampening"),
    EGGSISTOR_MINIATURIZATION("eggsistor"),
    NEURAL_NET_REFINEMENT("neural_net_refine"),

    // Tier 13
    MATTER_RECONFIGURATION("matter_reconfig"),
    TIMELINE_SPLICING("timeline_splicing"),
}
