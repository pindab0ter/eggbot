package nl.pindab0ter.eggbot.commands.rollcall

import com.auxbrain.ei.EggInc
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.sumBy
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction

object PaddingDistribution : DistributionAlgorithm() {
    override fun createRollCall(farmers: List<Farmer>, contract: EggInc.Contract): List<Coop> {
        val coops = createCoops(farmers, contract)
        val sortedFarmers = farmers.sortedByDescending { it.activeEarningsBonus }

        transaction {
            // Fill each co-op with the next strongest player so that all co-ops have one
            coops.forEachIndexed { i, coop ->
                coop.farmers = SizedCollection(coop.farmers.plus(sortedFarmers[i]))
            }

            // With the remaining farmers
            sortedFarmers.drop(coops.size).forEach { farmer ->
                // Keep adding the next highest rated farmer to the lowest rated co-op
                // Until a co-op is filled for 80%
                coops
                    .filter { coop ->
                        coop.farmers.count() < contract.maxCoopSize * 0.8
                    }
                    .sortedBy { coop ->
                        coop.farmers.sumBy { it.activeEarningsBonus }
                    }
                    .first()
                    .let { coop ->
                        coop.farmers = SizedCollection(coop.farmers.plus(farmer))
                    }
            }
        }

        return coops
    }

}
