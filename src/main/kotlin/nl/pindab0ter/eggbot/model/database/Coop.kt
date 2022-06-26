package nl.pindab0ter.eggbot.model.database

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.math.BigDecimal


@Suppress("unused")
class Coop(id: EntityID<Int>) : IntEntity(id) {
    var name by Coops.name
    var contractId by Coops.contractId

    private var _roleId by Coops.roleId
    var roleId: Snowflake?
        get() = this._roleId?.let { Snowflake(it) }
        set(snowflake) {
            _roleId = snowflake?.toString()
        }

    private var _channelId by Coops.channelId
    var channelId: Snowflake?
        get() = this._channelId?.let { Snowflake(it) }
        set(snowflake) {
            _channelId = snowflake?.toString()
        }

    var farmers by Farmer via CoopFarmers

    val activeEarningsBonus: BigDecimal
        get() = farmers.sumOf { farmer ->
            if (farmer.isActive) farmer.earningsBonus
            else BigDecimal.ZERO
        }

    var createdAt by Coops.createdAt
    var updatedAt by Coops.updatedAt

    companion object : IntEntityClass<Coop>(Coops)
}
