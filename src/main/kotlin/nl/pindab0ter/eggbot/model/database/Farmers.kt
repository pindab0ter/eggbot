package nl.pindab0ter.eggbot.model.database

import org.jetbrains.exposed.dao.IdTable
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.joda.time.DateTime

object Farmers : IdTable<String>() {
    override val tableName = "farmers"
    override val id = text("egg_inc_id").entityId().primaryKey()
    val discordId = reference("discord_id", DiscordUsers, CASCADE, CASCADE)
    val inGameName = text("in_game_name")
    val soulEggs = double("soul_eggs")
    val soulBonus = integer("soul_bonus")
    val prophecyEggs = long("prophecy_eggs")
    val prophecyBonus = integer("prophecy_bonus")
    val prestiges = long("prestiges")
    val droneTakedowns = long("drone_takedowns")
    val eliteDroneTakedowns = long("elite_drone_takedowns")
    val createdAt = datetime("created_at").default(DateTime.now())
    val updatedAt = datetime("updated_at").default(DateTime.now())
}
