package nl.pindab0ter.eggbot.model.database

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.UserBehavior
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.model.Config
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID
import org.joda.time.DateTime

class DiscordUser(id: EntityID<String>) : Entity<String>(id) {
    val snowflake: Snowflake = Snowflake(this.id.value)
    // TODO: Remove tag from database
    var tag by DiscordUsers.tag

    var inactiveUntil by DiscordUsers.inactiveUntil
    val isActive: Boolean get() = inactiveUntil?.isBeforeNow ?: true

    var createdAt by DiscordUsers.createdAt
    var updatedAt by DiscordUsers.updated_at

    val farmers by Farmer referrersOn Farmers.discordId

    companion object : EntityClass<String, DiscordUser>(DiscordUsers) {
        fun findBySnowflake(snowflake: Snowflake): DiscordUser? = DiscordUser.find { DiscordUsers.id eq snowflake.toString() }.firstOrNull()
        fun findOrCreate(user: UserBehavior): DiscordUser = runBlocking {
            val discordUser = user.asMember(Config.guild)
            val databaseUser = findBySnowflake(user.id) ?: DiscordUser.new(user.id.toString()) {
                tag = discordUser.tag
                createdAt = DateTime.now()
            }
            return@runBlocking databaseUser
        }
    }
}

