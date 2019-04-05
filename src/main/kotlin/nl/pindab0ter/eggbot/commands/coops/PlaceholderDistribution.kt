package nl.pindab0ter.eggbot.commands.coops

import com.auxbrain.ei.EggInc
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.Farmer
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction

object PlaceholderDistribution : DistributionAlgorithm {
    override fun createCoops(contract: EggInc.Contract): List<Coop> = transaction {
        val farmers = Farmer.all()
//        val coops = List(((farmers.count() * 1.2) / contract.maxCoopSize).roundToInt()) { index ->
        val coops = List(3) { index ->
            Coop.new {
                this.contractId = contract.identifier
                this.name = "${'a'.plus(index)}cluckerz${contract.maxCoopSize}"
            }
        }

        commit()

        farmers.forEachIndexed { index, f ->
            coops[index % coops.size].let { coop ->
                coop.farmers = SizedCollection(coop.farmers.plus(f))
            }
        }

        coops
    }
}