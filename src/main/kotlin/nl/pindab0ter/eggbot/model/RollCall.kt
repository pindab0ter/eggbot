package nl.pindab0ter.eggbot.model

import nl.pindab0ter.eggbot.helpers.mapCartesianProducts
import nl.pindab0ter.eggbot.model.database.Farmer
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private const val FILL_PERCENTAGE = 0.8

/**
 * Create a list of co-op names to lists of farmers
 */
fun createRollCall(
    baseName: String,
    maxCoopSize: Int,
    database: Database?
): List<Pair<String, SizedCollection<Farmer>>> = transaction(database) {
    val farmers = Farmer.all().with(Farmer::discordUser).toList()
    val activeFarmers = farmers.filter { it.isActive }.sortedByDescending { it.earningsBonus }
    val inactiveFarmers = farmers.filter { !it.isActive }.sortedBy { it.earningsBonus }
    val preferredCoopSize: Int =
        if (maxCoopSize <= 10) maxCoopSize
        else (maxCoopSize * FILL_PERCENTAGE).roundToInt()

    // Fill each co-op with the strongest available player so that all co-ops have one
    val coops = coopNames((farmers.size / preferredCoopSize) + 1, baseName).mapIndexed { i, coopName ->
        coopName to mutableListOf(activeFarmers[i])
    }

    // With the remaining active farmers keep adding the next highest rated to the lowest rated co-op
    activeFarmers.drop(coops.size).forEach { activeFarmer ->
        coops
            // Filter out co-ops that have enough farmers
            .filter { (_, farmers) -> farmers.count() <= preferredCoopSize }
            // Filter out co-ops that have the current highest amount of farmers
            .filter { (_, farmers) ->
                farmers.count() == coops.minOfOrNull { (_, coopFarmers) -> coopFarmers.count() }
            }
            // Select the coop with the current lowest total earnings bonus
            .minByOrNull { (_, farmers) -> farmers.sumOf { it.earningsBonus } }
            // Add the farmer to the co-op
            ?.let { (_, farmers) -> farmers.add(activeFarmer) }
    }

    // Finally, spread inactive farmers over the coops
    inactiveFarmers.forEach { inactiveFarmer ->
        coops
            // Sort the coops by amount of farmers
            .sortedBy { (_, farmers) -> farmers.count() }
            // Get the coop with the least amount of inactive farmers or all
            .minByOrNull { (_, farmers) -> farmers.count { farmer -> !farmer.isActive } }
            ?.let { (_, farmers) -> farmers.add(inactiveFarmer) }
    }

    coops.map { (name, farmers) -> name to SizedCollection(farmers) }
}

private fun coopNames(amount: Int, baseName: String): List<String> = when {
    amount <= 26 -> ('a' until 'a' + amount).map { char -> "$char-$baseName" }
    else -> {
        val chunks = ceil(amount.div(26.0)).toInt()
        val chunkSize = floor(amount.toDouble().div(chunks)).toInt()
        val remainder = amount - chunks * chunkSize
        ('a' until 'a' + chunkSize).mapCartesianProducts(1..chunks) { char: Char, digit: Int ->
            "$char$digit$baseName"
        }.plus(('a' until 'a' + remainder).map { c -> "${c + chunkSize}$baseName" })
    }
}

