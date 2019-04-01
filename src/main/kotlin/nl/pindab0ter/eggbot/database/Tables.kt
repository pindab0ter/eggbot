package nl.pindab0ter.eggbot.database

import com.auxbrain.ei.EggInc
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IdTable
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.Table

object DiscordUsers : IdTable<String>() {
    override val id = text("discord_tag").uniqueIndex().entityId()
    val role = text("role").nullable()
    //    val role = enumeration("role", Roles::class)
}

object InGameNames : IntIdTable() {
    val discordTag = reference("discord_tag", DiscordUsers)
    val inGameName = text("in_game_name").uniqueIndex()
}

object FarmerCoops : Table() {
    val farmer = reference("farmer", DiscordUsers.id, CASCADE).primaryKey(0)
    val coop = reference("coop", Coops).primaryKey(1)
}

object Coops : IntIdTable() {
    val name = text("name")
    val contract = reference("contract", Contracts.id, CASCADE)
    val maxSize = integer("max_size")
}

object Contracts : IdTable<String>() {
    override val id: Column<EntityID<String>> = text("identifier").primaryKey().entityId()
    val title = text("name")
    val description = text("description")
    val egg = enumeration("egg", EggInc.Egg::class)
    val coopAllowed = bool("coop_allowed")
    val coopSize = integer("coop_size")
    val validUntil = double("valid_until")
    val duration = double("lengthSeconds")
}

object Goals : IntIdTable() {
    val contract = reference("contract", Contracts.id, CASCADE)
    val tier = integer("tier") // 1-3 (or 4?) Must be unique per contract
    val goal = integer("goal")
    val reward = text("reward")
}

enum class Roles(oom: Int) {
    Farmer(2),
    Farmer2(3),
    Farmer3(4),
    KiloFarmer(5),
    KiloFarmer2(6),
    KiloFarmer3(7),
    MegaFarmer(8),
    MegaFarmer2(9),
    MegaFarmer3(10),
    GigaFarmer(11),
    GigaFarmer2(12),
    GigaFarmer3(13),
    TeraFarmer(14),
    TeraFarmer2(15),
    TeraFarmer3(16),
    PetaFarmer(17),
    PetaFarmer2(18),
    PetaFarmer3(19)
}

