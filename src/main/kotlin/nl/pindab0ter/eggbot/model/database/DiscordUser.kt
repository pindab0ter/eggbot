package nl.pindab0ter.eggbot.model.database

import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.database.DiscordUsers
import nl.pindab0ter.eggbot.database.Farmers
import nl.pindab0ter.eggbot.helpers.guild
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID
import org.joda.time.DateTime

class DiscordUser(id: EntityID<String>) : Entity<String>(id) {
    val discordId: String get() = id.value
    var discordTag by DiscordUsers.discordTag
    val discordName: String get() = discordTag.substring(0, discordTag.length - 5)
    var inactiveUntil by DiscordUsers.inactiveUntil
    private var optedOutOfCoopLeadAt by DiscordUsers.optedOutOfCoopLeadAt
    val optedOutOfCoopLead: Boolean get() = optedOutOfCoopLeadAt != null
    val farmers by Farmer referrersOn Farmers.discordId

    val mention: String
        get() = runBlocking {
            guild?.getMemberOrNull(snowflake)?.asMemberOrNull()?.mention ?: discordName
        }
    val isActive: Boolean get() = inactiveUntil?.isBeforeNow ?: true
    val snowflake: Snowflake = Snowflake(discordId)

    fun updateTag() = runBlocking {
        guild?.getMemberOrNull(snowflake)?.asMemberOrNull()?.tag.takeIf { it != discordTag }?.let { tag ->
            discordTag = tag
        }
    }

    fun optOutOfCoopLead() {
        optedOutOfCoopLeadAt = DateTime.now()
    }

    fun optInToCoopLead() {
        optedOutOfCoopLeadAt = null
    }

    companion object : EntityClass<String, DiscordUser>(DiscordUsers)
}

