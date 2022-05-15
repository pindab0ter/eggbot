package nl.pindab0ter.eggbot.model.database

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.jodatime.datetime

object DiscordGuilds : IdTable<String>() {
    override val tableName = "discord_guilds"
    override val id = text("id").entityId()
    override val primaryKey = PrimaryKey(id)
    val name = text("name")
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}
