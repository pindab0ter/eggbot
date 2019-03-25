package nl.pindab0ter.eggbot

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.Table

object ColumnNames {
    const val farmerInGameName = "in_game_name"
    const val farmerDiscordTag = "discord_tag"
}

object Farmers : IntIdTable() {
    val discordTag = text(ColumnNames.farmerInGameName).uniqueIndex()
    val inGameName = text(ColumnNames.farmerDiscordTag).uniqueIndex()
    val role = text("role").nullable()
    //    val role = enumeration("role", Roles::class)
}

class Farmer(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Farmer>(Farmers)

    var discordTag by Farmers.discordTag
    var inGameName by Farmers.inGameName
    var role by Farmers.role
}

object FarmerCoops : Table() {
    val farmer = reference("farmer", Farmers).primaryKey(0)
    val coop = reference("coop", Coops).primaryKey(1)
}

object Coops : IntIdTable() {
    val name = text("name")
    val contract = reference("contract", Contracts, CASCADE)
    val maxSize = integer("max_size")
}

object Contracts : IntIdTable() {
    val title = text("title")
    val description = text("description")
    val eggType = text("egg_type")
    val size = integer("size")
    val maxCoopSize = integer("max_coop_size")
    val daysToComplete = integer("days_to_complete")
    val validTill = datetime("valid_till")
}

object Goals : IntIdTable() {
    val contract = reference("contract", Contracts)
    val tier = enumeration("tier", Tiers::class) // Must be unique per contract
    val goal = integer("goal")
    val reward = text("reward")
}

enum class Tiers(tier: Int) {
    First(1),
    Second(2),
    Third(3)
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
