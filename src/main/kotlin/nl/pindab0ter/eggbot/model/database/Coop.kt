package nl.pindab0ter.eggbot.model.database

import nl.pindab0ter.eggbot.database.CoopFarmers
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.helpers.sumByBigDecimal
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import java.math.BigDecimal


class Coop(id: EntityID<Int>) : IntEntity(id) {
    var name by Coops.name
    var contract by Coops.contract
    var roleId by Coops.role_id

    var farmers by Farmer via CoopFarmers

    val activeEarningsBonus: BigDecimal
        get() = farmers.sumByBigDecimal { farmer ->
            if (farmer.isActive) farmer.earningsBonus
            else BigDecimal.ZERO
        }

    companion object : IntEntityClass<Coop>(Coops)
}