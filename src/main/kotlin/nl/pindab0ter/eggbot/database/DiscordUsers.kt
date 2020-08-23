package nl.pindab0ter.eggbot.database

import org.jetbrains.exposed.dao.IdTable

object DiscordUsers : IdTable<String>() {
    override val id = text("discord_id").uniqueIndex().entityId()
    val discordTag = text("discord_tag").uniqueIndex()
    val inactiveUntil = datetime("inactive_until").nullable()
}
