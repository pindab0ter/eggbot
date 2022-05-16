package nl.pindab0ter.eggbot.model.database

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.jodatime.datetime
import org.joda.time.DateTime.now

object DiscordUsers : IdTable<String>() {
    override val tableName = "discord_users"
    override val id = text("id").entityId()
    override val primaryKey = PrimaryKey(id)
    val tag = text("tag")
    val inactiveUntil = datetime("inactive_until").nullable()
    val createdAt = datetime("created_at").clientDefault { now() }
    val updatedAt = datetime("updated_at").clientDefault { now() }
}
