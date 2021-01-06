package nl.pindab0ter.eggbot.model

import com.auxbrain.ei.Contract
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.database.Farmer
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.roundToInt

private const val FILL_PERCENTAGE = 0.8

fun createRollCall(
    farmers: List<Farmer>,
    contract: Contract,
    baseName: String,
    noRole: Boolean,
): List<Coop> {
    val activeFarmers = farmers.filter { it.isActive }.sortedByDescending { it.earningsBonus }
    val inactiveFarmers = farmers.filter { !it.isActive }.sortedBy { it.earningsBonus }
    val preferredCoopSize: Int =
        if (contract.maxCoopSize <= 10) contract.maxCoopSize
        else (contract.maxCoopSize * FILL_PERCENTAGE).roundToInt()
    val coops = createCoopsAndRoles(contract, (farmers.size / preferredCoopSize) + 1, baseName, noRole)

    transaction {
        // Fill each co-op with the next strongest player so that all co-ops have one
        coops.forEachIndexed { i, coop ->
            coop.farmers = SizedCollection(coop.farmers.plus(activeFarmers[i]))
        }

        // With the remaining active farmers keep adding the next highest rated to the lowest rated co-op
        activeFarmers.drop(coops.size).forEach { activeFarmer ->
            coops.filter { coop -> coop.farmers.count() <= preferredCoopSize }
                .filter { coop -> coop.farmers.count() == coops.map { it.farmers.count() }.minOrNull() }
                .minByOrNull { coop -> coop.farmers.sumOf { it.earningsBonus } }!!
                .let { coop -> coop.farmers = SizedCollection(coop.farmers.plus(activeFarmer)) }
        }

        // Finally spread inactive farmers over the coops
        inactiveFarmers.forEach { inactiveFarmer ->
            coops.sortedBy { coop -> coop.farmers.count() }
                .minByOrNull { coop -> coop.farmers.count { farmer -> !farmer.isActive } }!!
                .let { coop -> coop.farmers = SizedCollection(coop.farmers.plus(inactiveFarmer)) }
        }
    }
    return coops
}
