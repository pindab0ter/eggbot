package nl.pindab0ter.eggbot.model.database

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.entity.Guild
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.helpers.kord
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.and

class DiscordUser(id: EntityID<String>) : Entity<String>(id) {
    val snowflake: Snowflake get() = Snowflake(this.id.value)
    var tag: String by DiscordUsers.tag

    var inactiveUntil by DiscordUsers.inactiveUntil
    val isActive: Boolean get() = inactiveUntil?.isBeforeNow ?: true

    var createdAt by DiscordUsers.createdAt
    var updatedAt by DiscordUsers.updatedAt

    val farmers by Farmer referrersOn Farmers.discordId
    var discordGuild by DiscordGuild referencedOn DiscordUsers.guildId

    val mention: String?
        get() = runBlocking {
            discordGuild.asGuild()?.getMemberOrNull(this@DiscordUser.snowflake)?.mention
                ?: kord.getUser(this@DiscordUser.snowflake)?.mention
        }

    companion object : EntityClass<String, DiscordUser>(DiscordUsers) {
        fun find(snowflake: Snowflake, discordGuild: Guild): DiscordUser? = DiscordUser.find {
            (DiscordUsers.id eq snowflake.toString()) and (DiscordUsers.guildId eq discordGuild.id.toString())
        }.firstOrNull()

        fun findOrCreate(user: UserBehavior, guild: Guild): DiscordUser {
            val discordUser = find(user.id, guild)
            if (discordUser != null) return discordUser

            val discordGuild = DiscordGuild.findOrCreate(guild)

            return new(user.id.toString()) {
                this.tag = runBlocking { user.asUser().tag }
                this.discordGuild = discordGuild
            }
        }
    }
}

