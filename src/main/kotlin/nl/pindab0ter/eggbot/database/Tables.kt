package nl.pindab0ter.eggbot.database

import org.jetbrains.exposed.dao.IdTable
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.Table

object DiscordUsers : IdTable<String>() {
    override val id = text("discord_id").uniqueIndex().entityId()
    val discordTag = text("discord_tag").uniqueIndex()
}

object Farmers : IdTable<String>() {
    override val id = text("in_game_id").uniqueIndex().entityId()
    val discordId = reference("discord_id", DiscordUsers)
    val inGameName = text("in_game_name").uniqueIndex()
    val soulEggs = long("soul_eggs")
    val prophecyEggs = long("prophecy_eggs")
    val soulBonus = integer("soul_bonus")
    val prophecyBonus = integer("prophecy_bonus")
}

object Coops : IntIdTable() {
    val name = text("name")
    val contractId = text("contract_id")
    val hasStarted = bool("has-started").default(false).nullable()

    init {
        this.index(true, name, contractId)
    }
}

object CoopFarmers : Table() {
    val farmer = reference("farmer", Farmers.id, CASCADE)
    val coop = reference("coop", Coops.id)

    init {
        this.index(true, CoopFarmers.farmer, CoopFarmers.coop)
    }
}
