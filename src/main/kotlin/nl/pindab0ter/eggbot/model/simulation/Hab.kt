package nl.pindab0ter.eggbot.model.simulation

import com.auxbrain.ei.Backup
import nl.pindab0ter.eggbot.helpers.capacity
import nl.pindab0ter.eggbot.helpers.habCapacityMultipliers
import nl.pindab0ter.eggbot.helpers.product
import java.math.BigDecimal

data class Hab(
    val population: BigDecimal,
    val capacity: BigDecimal,
) {
    companion object {
        fun fromFarm(farm: Backup.Simulation) = (0..3).map { index ->
            Hab(
                population = farm.habPopulation[index].toBigDecimal(),
                capacity = farm.habs[index].capacity.multiply(farm.habCapacityMultipliers.product())
            )
        }
    }
}