package nl.pindab0ter.eggbot.model

import nl.pindab0ter.eggbot.helpers.mapCartesianProducts
import nl.pindab0ter.eggbot.model.database.Farmer
import kotlin.math.ceil
import kotlin.math.floor


/**
 * Create a list of co-op names to lists of farmers
 */
fun createRollCall(
    baseName: String,
    preferredCoopSize: Int,
    farmers: List<Farmer>,
): List<Pair<String, List<Farmer>>> {
    // Fill each co-op with the strongest available player so that all co-ops have one
    val coops = coopNames(farmers.size / preferredCoopSize, baseName).mapIndexed { i, coopName ->
        coopName to mutableListOf(farmers[i])
    }

    // With the remaining active farmers keep adding the next highest rated to the lowest rated co-op
    farmers.drop(coops.size).forEach { activeFarmer ->
        coops
            // Filter out co-ops that have enough farmers
            .filter { (_, farmers) -> farmers.count() <= preferredCoopSize }
            // Filter out co-ops that have the current highest amount of farmers
            .filter { (_, farmers) -> farmers.count() == coops.minOfOrNull { (_, coopFarmers) -> coopFarmers.count() } }
            // Select the coop with the current lowest total earnings bonus
            .minByOrNull { (_, farmers) -> farmers.sumOf { it.earningsBonus } }
            // Add the farmer to the co-op
            ?.let { (_, farmers) -> farmers.add(activeFarmer) }
    }

    return coops.map { (name, farmers) -> name to farmers }
}

private fun coopNames(amount: Int, baseName: String): List<String> = when {
    amount <= 26 -> ('a' until 'a' + amount).map { char -> "$char-$baseName" }
    else -> {
        val chunks = ceil(amount.div(26.0)).toInt()
        val chunkSize = floor(amount.toDouble().div(chunks)).toInt()
        val remainder = amount - chunks * chunkSize
        ('a' until 'a' + chunkSize).mapCartesianProducts(1..chunks) { char: Char, digit: Int ->
            "$char$digit$baseName"
        }.plus(('a' until 'a' + remainder).map { c -> "${c + chunkSize}-$baseName" })
    }
}

