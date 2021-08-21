package nl.pindab0ter.eggbot.model.database

import dev.kord.common.entity.Snowflake
import nl.pindab0ter.eggbot.database.CoopFarmers
import nl.pindab0ter.eggbot.database.Coops
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import java.math.BigDecimal


class Coop(id: EntityID<Int>) : IntEntity(id) {
    var name by Coops.name
    private var _roleId by Coops.roleId
    var roleId: Snowflake?
        get() = this._roleId?.let { Snowflake(it) }
        set(snowflake) {
            _roleId = snowflake?.asString
        }
    var contractId by Coops.contractId

    var leader: Farmer? by Farmer optionalReferencedOn Coops.leaderId
    var farmers by Farmer via CoopFarmers

    val hasLeader: Boolean get() = leader != null

    val activeEarningsBonus: BigDecimal
        get() = farmers.sumOf { farmer ->
            if (farmer.isActive) farmer.earningsBonus
            else BigDecimal.ZERO
        }

    companion object : IntEntityClass<Coop>(Coops)
}
