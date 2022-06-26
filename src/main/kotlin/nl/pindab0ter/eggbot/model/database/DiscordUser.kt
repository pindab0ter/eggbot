package nl.pindab0ter.eggbot.model.database

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.UserBehavior
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

@Suppress("unused")
class DiscordUser(id: EntityID<String>) : Entity<String>(id) {
    val snowflake: Snowflake get() = Snowflake(this.id.value)
    var tag: String by DiscordUsers.tag

    var inactiveUntil by DiscordUsers.inactiveUntil
    val isActive: Boolean get() = inactiveUntil?.isBeforeNow ?: true

    var createdAt by DiscordUsers.createdAt
    var updatedAt by DiscordUsers.updatedAt

    val farmers by Farmer referrersOn Farmers.discordUserId

    companion object : EntityClass<String, DiscordUser>(DiscordUsers) {
        fun findBy(snowflake: Snowflake): DiscordUser? = DiscordUser
            .find { DiscordUsers.id eq snowflake.toString() }
            .firstOrNull()

        fun findOrCreate(user: UserBehavior): DiscordUser {
            val discordUser = findBy(user.id)
            if (discordUser != null) return discordUser

            return new(user.id.toString()) {
                this.tag = runBlocking { user.asUser().tag }
            }
        }
    }
}

