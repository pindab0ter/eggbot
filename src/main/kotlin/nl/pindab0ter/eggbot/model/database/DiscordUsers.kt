package nl.pindab0ter.eggbot.model.database

import org.jetbrains.exposed.dao.IdTable

object DiscordUsers : IdTable<String>() {
    override val tableName = "discord_users"
    override val id = text("id").entityId().primaryKey()
    val tag = text("tag")
    val inactiveUntil = datetime("inactive_until").nullable()
    val createdAt = datetime("created_at")
    val updated_at = datetime("updated_at")
}
