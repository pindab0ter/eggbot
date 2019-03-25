package nl.pindab0ter.eggbot

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE

object ColumnNames {
    const val farmerInGameName = "in_game_name"
    const val farmerDiscordTag = "discord_tag"
}

object Farmers : IntIdTable() {
    val discordTag = text(ColumnNames.farmerInGameName).uniqueIndex()
    val inGameName = text(ColumnNames.farmerDiscordTag).uniqueIndex()
    val role = text("role")
}

class Farmer(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Farmer>(Farmers)

    var discordTag by Farmers.discordTag
    var inGameName by Farmers.inGameName
    var role by Farmers.role
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
    val tiers = reference("tiers", Tiers, CASCADE)
    val maxCoopSize = integer("max_coop_size")
    val daysToComplete = integer("days_to_complete")
    val validTill = datetime("valid_till")
}

object Tiers : IntIdTable() {
    val tier = integer("tier") // 1, 2 or 3, **must be unique per contract**
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
