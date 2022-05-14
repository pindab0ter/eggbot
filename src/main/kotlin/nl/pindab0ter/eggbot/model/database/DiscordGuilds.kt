package nl.pindab0ter.eggbot.model.database

import org.jetbrains.exposed.dao.IdTable

object DiscordGuilds : IdTable<String>() {
    override val tableName = "discord_guilds"
    override val id = text("id").entityId().primaryKey()
    val name = text("name")
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}
