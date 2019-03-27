package nl.pindab0ter.eggbot.database

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.Table

object ColumnNames {
    const val FARMER_IN_GAME_NAME = "in_game_name"
    const val FARMER_DISCORD_TAG = "discord_tag"
}

object Farmers : IntIdTable() {
    val discordTag = text(ColumnNames.FARMER_IN_GAME_NAME).uniqueIndex()
    val inGameName = text(ColumnNames.FARMER_DISCORD_TAG).uniqueIndex()
    val role = text("role").nullable()
    //    val role = enumeration("role", Roles::class)
}

object FarmerCoops : Table() {
    val farmer = reference("farmer", Farmers).primaryKey(0)
    val coop = reference("coop", Coops).primaryKey(1)
}

object Coops : IntIdTable() {
    val name = text("name")
    val contract = reference("contract", Contracts.identifier, CASCADE)
    val maxSize = integer("max_size")
}

object Contracts : Table() {
    val identifier = text("identifier").primaryKey()
    val title = text("title")
    val description = text("description")
    val egg = enumeration("egg", Egg::class)
    val coopAllowed = bool("coop_allowed")
    val coopSize = integer("coop_size")
    val validUntil = datetime("valid_until")
    val duration = double("duration")
}

object Goals : IntIdTable() {
    val contract = reference("contract", Contracts.identifier, CASCADE)
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

enum class Egg(id: Int) {
    DEFAULT(0),
    EDIBLE(1),
    SUPERFOOD(2),
    MEDICAL(3),
    ROCKET_FUEL(4),
    SUPER_MATERIAL(5),
    FUSION(6),
    QUANTUM(7),
    IMMORTALITY(8),
    TACHYON(9),
    GRAVITON(10),
    DILITHIUM(11),
    PRODIGY(12),
    TERRAFORM(13),
    ANTIMATTER(14),
    DARK_MATTER(15),
    AI(16),
    NEBULA(17),
    CHOCOLATE(100),
    EASTER(101),
    WATER_BALLOON(102),
    FIREWORK(103),
    PUMPKIN(104)
}
