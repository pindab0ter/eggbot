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

        transaction {
            coops.forEachIndexed { i, coop ->
                coop.farmers = SizedCollection(coop.farmers.plus(farmers[i]))
            }

            farmers.drop(coops.size).forEach { farmer ->
                coops
                    .filter { coop -> coop.farmers.count() < contract.maxCoopSize * 0.8 }
                    .sortedBy { coop -> coop.farmers.sumBy { it.earningsBonus } }
                    .first()
                    .let { coop ->
                        coop.farmers = SizedCollection(coop.farmers.plus(farmer))
                    }
            }
        }

        return coops
    }

}
