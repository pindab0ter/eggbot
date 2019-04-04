package nl.pindab0ter.eggbot.database

import com.auxbrain.ei.EggInc
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IdTable
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column
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

object FarmerCoops : Table() {
    val farmer = reference("farmer", DiscordUsers.id, CASCADE).primaryKey(0)
    val coop = reference("coop", Coops).primaryKey(1)
}

object Coops : IntIdTable() {
    val name = text("name")
    val hasStarted = bool("has-started")
}
}

