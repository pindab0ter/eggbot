package nl.pindab0ter.eggbot.model.database

import nl.pindab0ter.eggbot.database.Contracts
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID
import java.math.BigDecimal

class Contract(id: EntityID<String>) : Entity<String>(id) {
    var name by Contracts.name
    private var finalGoalDouble: Double by Contracts.finalGoal
    var finalGoal: BigDecimal
        get() = finalGoalDouble.toBigDecimal()
        set(value) {
            finalGoalDouble = value.toDouble()
        }

    companion object : EntityClass<String, Contract>(Contracts)
}