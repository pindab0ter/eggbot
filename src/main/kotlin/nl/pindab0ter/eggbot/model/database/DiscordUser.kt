package nl.pindab0ter.eggbot.model.database

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID

class DiscordUser(id: EntityID<String>) : Entity<String>(id) {
    val snowflake: Snowflake = Snowflake(this.id.value)
    var tag by DiscordUsers.tag

    var inactiveUntil by DiscordUsers.inactiveUntil
    val isActive: Boolean get() = inactiveUntil?.isBeforeNow ?: true

    var createdAt by DiscordUsers.createdAt
    var updatedAt by DiscordUsers.updated_at

    val farmers by Farmer referrersOn Farmers.discordId

    companion object : EntityClass<String, DiscordUser>(DiscordUsers)
}

