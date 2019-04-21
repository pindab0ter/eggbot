package nl.pindab0ter.eggbot.commands.rollcall

import nl.pindab0ter.eggbot.database.Contract
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.Farmer
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction

object SequentialDistribution : DistributionAlgorithm() {
    override fun createRollCall(farmers: List<Farmer>, contract: Contract): List<Coop> {
        val coops = createCoops(farmers, contract)

        transaction {
            farmers.forEachIndexed { index, farmer ->
                coops[index % coops.size].let { coop ->
                    coop.farmers = SizedCollection(coop.farmers.plus(farmer))
                }
            }
        }

        return coops
    }
}
