package nl.pindab0ter.eggbot.model.simulation

import com.auxbrain.ei.Backup
import nl.pindab0ter.eggbot.utilities.capacity
import nl.pindab0ter.eggbot.utilities.habCapacityMultipliers
import nl.pindab0ter.eggbot.utilities.product
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