package nl.pindab0ter.eggbot.database

import org.jetbrains.exposed.dao.IdTable
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.Table
import org.joda.time.DateTime

object DiscordUsers : IdTable<String>() {
    override val id = text("discord_id").uniqueIndex().entityId()
    val discordTag = text("discord_tag").uniqueIndex()
    val isActive = bool("is_active").default(true)
}

object Farmers : IdTable<String>() {
    override val id = text("in_game_id").uniqueIndex().entityId()
    val discordId = reference("discord_id", DiscordUsers)
    val inGameName = text("in_game_name").uniqueIndex()
    val soulEggs = long("soul_eggs")
    val prophecyEggs = long("prophecy_eggs")
    val soulBonus = integer("soul_bonus")
    val prophecyBonus = integer("prophecy_bonus")
    val lastUpdated = datetime("last_updated").default(DateTime.now())
}

object Coops : IntIdTable() {
    val name = text("name")
    val contractId = text("contract_id")
    val hasStarted = bool("has-started").default(false)

    init {
        this.index(true, name, contractId)
    }
}

object CoopFarmers : Table() {
    val farmer = reference("farmer", Farmers.id, CASCADE)
    val coop = reference("coop", Coops.id)

    init {
        this.index(true, farmer, coop)
    }
}
