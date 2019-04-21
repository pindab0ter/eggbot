package nl.pindab0ter.eggbot.commands.rollcall

import nl.pindab0ter.eggbot.database.Contract
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.sumBy
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.roundToInt

object PaddingDistribution : DistributionAlgorithm() {
    private const val FILL_PERCENTAGE = 0.8

    override fun createRollCall(farmers: List<Farmer>, contract: Contract): List<Coop> {
        val coops = createCoops(farmers, contract)
        val activeFarmers = farmers.filter { it.isActive }.sortedByDescending { it.earningsBonus }
        val inactiveFarmers = farmers.filter { !it.isActive }
        val preferredCoopSize = {
            val inactiveToActiveFarmerRatio = inactiveFarmers.size.toFloat() / farmers.size.toFloat()
            val activeFarmerFillRatio = FILL_PERCENTAGE - inactiveToActiveFarmerRatio * FILL_PERCENTAGE
            (contract.maxCoopSize * activeFarmerFillRatio).roundToInt()
        }()

        transaction {
            // Fill each co-op with the next strongest player so that all co-ops have one
            coops.forEachIndexed { i, coop ->
                coop.farmers = SizedCollection(coop.farmers.plus(activeFarmers[i]))
            }

            // With the remaining active farmers keep adding the next highest rated to the lowest rated co-op
            activeFarmers.drop(coops.size).forEach { activeFarmer ->
                coops.filter { coop -> coop.farmers.count() < preferredCoopSize }
                    .sortedBy { coop -> coop.farmers.sumBy { it.earningsBonus } }
                    .first()
                    .let { coop -> coop.farmers = SizedCollection(coop.farmers.plus(activeFarmer)) }
            }

            // Finally spread inactive farmers over the coops
            inactiveFarmers.forEach { inactiveFarmer ->
                coops.sortedBy { coop -> coop.farmers.count() }
                    .sortedBy { coop -> coop.farmers.count { farmer -> !farmer.isActive } }
                    .first()
                    .let { coop -> coop.farmers = SizedCollection(coop.farmers.plus(inactiveFarmer)) }
            }
        }
        return coops
    }
}
