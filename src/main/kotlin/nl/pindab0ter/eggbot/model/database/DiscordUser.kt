package nl.pindab0ter.eggbot.model.database

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.UserBehavior
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.helpers.guild
import nl.pindab0ter.eggbot.helpers.kord
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID
import org.joda.time.DateTime

class DiscordUser(id: EntityID<String>) : Entity<String>(id) {
    val snowflake: Snowflake = Snowflake(this.id.value)
    var tag: String by DiscordUsers.tag

    var inactiveUntil by DiscordUsers.inactiveUntil
    val isActive: Boolean get() = inactiveUntil?.isBeforeNow ?: true

    var createdAt by DiscordUsers.createdAt
    var updatedAt by DiscordUsers.updated_at

    val farmers by Farmer referrersOn Farmers.discordId

    val mention: String?
        get() = runBlocking {
            guild?.getMemberOrNull(this@DiscordUser.snowflake)?.mention
                ?: kord.getUser(this@DiscordUser.snowflake)?.mention
        }

    companion object : EntityClass<String, DiscordUser>(DiscordUsers) {
        fun findBySnowflake(snowflake: Snowflake): DiscordUser? = DiscordUser.find { DiscordUsers.id eq snowflake.toString() }.firstOrNull()
        fun findOrCreate(user: UserBehavior): DiscordUser = findBySnowflake(user.id) ?: new(user.id.toString()) {
            tag = runBlocking { user.asUser().tag }
            createdAt = DateTime.now()
        }
    }
}

