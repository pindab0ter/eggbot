package nl.pindab0ter.eggbot.commands.rollcall

import com.auxbrain.ei.EggInc
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.sumBy
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.roundToInt

object PaddingDistribution : DistributionAlgorithm {
    override fun createCoops(contract: EggInc.Contract): List<Coop> = transaction {
        val farmers = Farmer.all().sortedByDescending { it.earningsBonus }.toMutableList()
        val coops = List(((farmers.count() * 1.2) / contract.maxCoopSize).roundToInt()) { index ->
            Coop.new {
                this.contractId = contract.identifier
                this.name = "${'a'.plus(index)}cluckerz${contract.maxCoopSize}"
            }
        }

        commit()

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

        coops
    }
}
