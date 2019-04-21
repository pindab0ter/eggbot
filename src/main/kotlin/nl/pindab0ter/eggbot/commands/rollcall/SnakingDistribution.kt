package nl.pindab0ter.eggbot.commands.rollcall

import com.auxbrain.ei.EggInc
import nl.pindab0ter.eggbot.database.Contract
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.Farmer
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction

object SnakingDistribution : DistributionAlgorithm() {
    override fun createRollCall(farmers: List<Farmer>, contract: Contract): List<Coop> {
        val coops = createCoops(farmers, contract)

        transaction {
            val count = coops.count()
            farmers.forEachIndexed { i, farmer ->
                val up = i % count                                     // 0, 1, 2, 0, 1, 2, 0, 1, 2…
                val down = (count - 1) + up + (-up) * 2                // 2, 1, 0, 2, 1, 0, 2, 1, 0…
                coops[if (i % (count * 2) > count - 1) down else up]   // 0, 1, 2, 2, 1, 0, 0, 1, 2…
                    .let { coop ->
                        coop.farmers = SizedCollection(coop.farmers.plus(farmer))
                    }
            }
        }

        return coops
    }
}
