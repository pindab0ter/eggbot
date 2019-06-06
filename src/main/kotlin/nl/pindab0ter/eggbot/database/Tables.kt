package nl.pindab0ter.eggbot.database

import org.jetbrains.exposed.dao.IdTable
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.ReferenceOption.NO_ACTION
import org.jetbrains.exposed.sql.Table
import org.joda.time.DateTime

object DiscordUsers : IdTable<String>() {
    override val id = text("discord_id").uniqueIndex().entityId()
    val discordTag = text("discord_tag").uniqueIndex()
    val inactiveUntil = datetime("inactive_until").nullable()
}

object Farmers : IdTable<String>() {
    override val id = text("in_game_id").uniqueIndex().entityId()
    val discordId = reference("discord_id", DiscordUsers, CASCADE, NO_ACTION)
    val inGameName = text("in_game_name").uniqueIndex()
    val soulEggs = long("soul_eggs")
    val prophecyEggs = long("prophecy_eggs")
    val soulBonus = integer("soul_bonus")
    val prophecyBonus = integer("prophecy_bonus")
    val prestiges = integer("prestiges")
    val droneTakedowns = integer("drone_takedowns")
    val eliteDroneTakedowns = integer("elite_drone_takedowns")
    val lastUpdated = datetime("last_updated").default(DateTime.now())
}

object Coops : IntIdTable() {
    val name = text("name")
    val contract = text("contract_id")

    init {
        this.index(true, name, contract)
    }
}

object CoopFarmers : Table() {
    val farmer = reference("farmer", Farmers, CASCADE, NO_ACTION)
    val coop = reference("coop", Coops, CASCADE, NO_ACTION)

    init {
        this.index(true, farmer, coop)
    }
}
