package nl.pindab0ter.eggbot.database

import org.jetbrains.exposed.dao.IdTable
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.ReferenceOption.SET_NULL
import org.jetbrains.exposed.sql.Table
import org.joda.time.DateTime

object DiscordUsers : IdTable<String>() {
    override val id = text("discord_id").uniqueIndex().entityId()
    val discordTag = text("discord_tag").uniqueIndex()
    val inactiveUntil = datetime("inactive_until").nullable()
}

object Farmers : IdTable<String>() {
    override val id = text("in_game_id").uniqueIndex().entityId()
    val discordId = reference("discord_id", DiscordUsers, CASCADE, CASCADE)
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

object Coops : IntIdTable() {
    val name = text("name")
    val contract = text("contract_id")
    val role_id = text("role_id").nullable()

    init {
        this.index(true, name, contract)
    }
}

object CoopFarmers : Table() {
    val farmer = reference("farmer", Farmers, SET_NULL, CASCADE)
    val coop = reference("coop", Coops, SET_NULL, CASCADE)

    init {
        this.index(true, farmer, coop)
    }
}
