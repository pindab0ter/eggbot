package nl.pindab0ter.eggbot.model.database

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Role
import dev.kord.core.entity.channel.Channel
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import java.math.BigDecimal


class Coop(id: EntityID<Int>) : IntEntity(id) {
    var name by Coops.name
    var contractId by Coops.contractId

    private var _roleId by Coops.roleId
    var roleId: Snowflake?
        get() = this._roleId?.let { Snowflake(it) }
        set(snowflake) {
            _roleId = snowflake?.toString()
        }
    val role: Role?
        get() = this@Coop.roleId?.let {
            runBlocking { discordGuild.asGuild()?.getRoleOrNull(it) }
        }

    private var _channelId by Coops.channelId
    var channelId: Snowflake?
        get() = this._channelId?.let { Snowflake(it) }
        set(snowflake) {
            _channelId = snowflake?.toString()
        }
    val channel: Channel?
        get() = this@Coop.channelId?.let {
            runBlocking { discordGuild.asGuild()?.getChannelOrNull(it) }
        }

    var farmers by Farmer via CoopFarmers
    val discordGuild by DiscordGuild referencedOn DiscordUsers.guildId

    val activeEarningsBonus: BigDecimal
        get() = farmers.sumOf { farmer ->
            if (farmer.isActive) farmer.earningsBonus
            else BigDecimal.ZERO
        }

    var createdAt by Coops.createdAt
    var updatedAt by Coops.updatedAt

    companion object : IntEntityClass<Coop>(Coops)
}
