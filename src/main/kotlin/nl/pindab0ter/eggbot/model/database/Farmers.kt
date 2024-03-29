package nl.pindab0ter.eggbot.model.database

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.jodatime.datetime
import org.joda.time.DateTime.now

object Farmers : IdTable<String>() {
    override val tableName = "farmers"
    override val id = text("egg_inc_id").entityId()
    override val primaryKey = PrimaryKey(id)
    val discordUserId = reference("discord_user_id", DiscordUsers, CASCADE, CASCADE)
    val inGameName = text("in_game_name").nullable()
    val soulEggs = double("soul_eggs")
    val soulBonus = integer("soul_bonus")
    val prophecyEggs = long("prophecy_eggs")
    val prophecyBonus = integer("prophecy_bonus")
    val prestiges = long("prestiges")
    val droneTakedowns = long("drone_takedowns")
    val eliteDroneTakedowns = long("elite_drone_takedowns")
    val rocketsLaunched = integer("rockets_launched")
    val createdAt = datetime("created_at").clientDefault { now() }
    val updatedAt = datetime("updated_at").clientDefault { now() }
}
