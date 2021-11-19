package nl.pindab0ter.eggbot.model.database

import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.helpers.configuredGuild
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID
import org.joda.time.DateTime

class DiscordUser(id: EntityID<String>) : Entity<String>(id) {
    private val discordId: String get() = id.value
    val snowflake: Snowflake = Snowflake(discordId)

    suspend fun asMemberOrNull() = configuredGuild?.getMemberOrNull(snowflake)?.asMemberOrNull()

    var discordTag by DiscordUsers.discordTag

    var inactiveUntil by DiscordUsers.inactiveUntil
    val isActive: Boolean get() = inactiveUntil?.isBeforeNow ?: true

    private var optedOutOfCoopLeadAt by DiscordUsers.optedOutOfCoopLeadAt
    val optedOutOfCoopLead: Boolean get() = optedOutOfCoopLeadAt != null

    var createdAt by DiscordUsers.createdAt
    var updatedAt by DiscordUsers.updated_at

    val farmers by Farmer referrersOn Farmers.discordId

    suspend fun updateTag() = asMemberOrNull()?.tag.takeIf { it != discordTag }?.let { tag ->
        discordTag = tag
    }

    companion object : EntityClass<String, DiscordUser>(DiscordUsers)
}

