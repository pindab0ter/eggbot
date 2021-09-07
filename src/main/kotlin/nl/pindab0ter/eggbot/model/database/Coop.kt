package nl.pindab0ter.eggbot.model.database

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import java.math.BigDecimal


class Coop(id: EntityID<Int>) : IntEntity(id) {
    var name by Coops.name
    var contractId by Coops.contractId
    var leader: Farmer? by Farmer optionalReferencedOn Coops.leaderId
    private var _roleId by Coops.roleId
    var roleId: Snowflake?
        get() = this._roleId?.let { Snowflake(it) }
        set(snowflake) {
            _roleId = snowflake?.asString
        }
    private var _channelId by Coops.channelId
    var channelId: Snowflake?
        get() = this._channelId?.let { Snowflake(it) }
        set(snowflake) {
            _channelId = snowflake?.asString
        }
    var createdAt by Coops.createdAt
    var updatedAt by Coops.updated_at

    var farmers by Farmer via CoopFarmers

    val hasLeader: Boolean get() = leader != null
    val activeEarningsBonus: BigDecimal
        get() = farmers.sumOf { farmer ->
            if (farmer.isActive) farmer.earningsBonus
            else BigDecimal.ZERO
        }

    companion object : IntEntityClass<Coop>(Coops)
}
