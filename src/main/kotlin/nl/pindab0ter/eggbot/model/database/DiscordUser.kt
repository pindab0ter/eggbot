package nl.pindab0ter.eggbot.model.database

import nl.pindab0ter.eggbot.EggBot.guild
import nl.pindab0ter.eggbot.database.DiscordUsers
import nl.pindab0ter.eggbot.database.Farmers
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

    val asMention: String get() = guild.getMemberById(discordId)?.asMention ?: discordName
    val isActive: Boolean get() = inactiveUntil?.isBeforeNow ?: true

    fun updateTag() = guild.getMemberById(discordId)?.user?.asTag.takeIf { it != discordTag }?.let { tag ->
        discordTag = tag
    }

    fun optOutOfCoopLead() {
        optedOutOfCoopLeadAt = DateTime.now()
    }

    fun optInToCoopLead() {
        optedOutOfCoopLeadAt = null
    }

    companion object : EntityClass<String, DiscordUser>(DiscordUsers)
}

