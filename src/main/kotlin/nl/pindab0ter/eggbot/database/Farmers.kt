package nl.pindab0ter.eggbot.database

import org.jetbrains.exposed.dao.IdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.joda.time.DateTime

object Farmers : IdTable<String>() {
    override val id = text("in_game_id").uniqueIndex().entityId()
    val discordId = reference("discord_id", DiscordUsers, ReferenceOption.CASCADE, ReferenceOption.CASCADE)
    val inGameName = text("in_game_name").uniqueIndex()
    val soulEggsLong = long("soul_eggs_long")
    val soulEggsDouble = double("soul_eggs_double")
    val prophecyEggs = long("prophecy_eggs")
    val soulBonus = integer("soul_bonus")
    val prophecyBonus = integer("prophecy_bonus")
    val prestiges = long("prestiges")
    val droneTakedowns = long("drone_takedowns")
    val eliteDroneTakedowns = long("elite_drone_takedowns")
    val lastUpdated = datetime("last_updated").default(DateTime.now())
}
