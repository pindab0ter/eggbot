package nl.pindab0ter.eggbot.model.database

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.jodatime.datetime

object DiscordUsers : IdTable<String>() {
    override val tableName = "discord_users"
    override val id = text("id").entityId()
    override val primaryKey = PrimaryKey(id)
    val tag = text("tag")
    val inactiveUntil = datetime("inactive_until").nullable()
    val guildId = reference("guild_id", DiscordGuilds, CASCADE, CASCADE)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}
