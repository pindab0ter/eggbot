package nl.pindab0ter.eggbot

import com.auxbrain.ei.EggInc

class FarmCalculator(val backup: EggInc.Backup) {

    private val farm = backup.farmsList.find { it.farmType == EggInc.FarmType.HOME }!!

    val eggLayingBonus = listOf(
        1 + .10 * farm.commonResearchList[CommonResearch.COMFORTABLE_NESTS.ordinal].level,
        1 + .05 * farm.commonResearchList[CommonResearch.HEN_HOUSE_AC.ordinal].level,
        1 + .15 * farm.commonResearchList[CommonResearch.IMPROVED_GENETICS.ordinal].level,
        1 + .10 * farm.commonResearchList[CommonResearch.TIME_COMPRESSION.ordinal].level,
        1 + .02 * farm.commonResearchList[CommonResearch.TIMELINE_DIVERSION.ordinal].level,
        1 + .05 * backup.data.epicResearchList[EpicResearch.EPIC_COMFY_NESTS.ordinal].level
    )
}

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
