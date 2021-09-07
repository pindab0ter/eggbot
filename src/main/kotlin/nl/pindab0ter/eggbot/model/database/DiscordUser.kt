package nl.pindab0ter.eggbot.model.database

import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.helpers.configuredGuild
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID
import org.joda.time.DateTime

class DiscordUser(id: EntityID<String>) : Entity<String>(id) {
    val discordId: String get() = id.value
    var discordTag by DiscordUsers.discordTag
    var inactiveUntil by DiscordUsers.inactiveUntil
    private var optedOutOfCoopLeadAt by DiscordUsers.optedOutOfCoopLeadAt
    val optedOutOfCoopLead: Boolean get() = optedOutOfCoopLeadAt != null
    var createdAt by DiscordUsers.createdAt
    var updatedAt by DiscordUsers.modifiedAt

    val farmers by Farmer referrersOn Farmers.discordId

    val discordName: String get() = discordTag.substring(0, discordTag.length - 5)
    val mention: String
        get() = runBlocking {
            configuredGuild?.getMemberOrNull(snowflake)?.asMemberOrNull()?.mention ?: discordName
        }
    val isActive: Boolean get() = inactiveUntil?.isBeforeNow ?: true
    val snowflake: Snowflake = Snowflake(discordId)

    fun updateTag() = runBlocking {
        configuredGuild?.getMemberOrNull(snowflake)?.asMemberOrNull()?.tag.takeIf { it != discordTag }?.let { tag ->
            discordTag = tag
        }
    }

    companion object : EntityClass<String, DiscordUser>(DiscordUsers)
}

