package nl.pindab0ter.eggbot.model.database

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.math.BigDecimal


class Coop(id: EntityID<Int>) : IntEntity(id) {
    var name by Coops.name
    var contractId by Coops.contractId

    private var _roleSnowflake by Coops.roleSnowflake
    var roleSnowflake: Snowflake?
        get() = this._roleSnowflake?.let { Snowflake(it) }
        set(snowflake) {
            _roleSnowflake = snowflake?.toString()
        }

    private var _channelSnowflake by Coops.channelSnowflake
    var channelSnowflake: Snowflake?
        get() = this._channelSnowflake?.let { Snowflake(it) }
        set(snowflake) {
            _channelSnowflake = snowflake?.toString()
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
