package nl.pindab0ter.eggbot.database

import com.auxbrain.ei.EggInc
import org.jetbrains.exposed.dao.IdTable
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.Table
import org.joda.time.DateTime

object DiscordUsers : IdTable<String>() {
    override val id = text("discord_id").uniqueIndex().entityId()
    val discordTag = text("discord_tag").uniqueIndex()
    val inactiveUntil = datetime("inactive_until").nullable()
}

object Farmers : IdTable<String>() {
    override val id = text("in_game_id").uniqueIndex().entityId()
    val discordId = reference("discord_id", DiscordUsers, CASCADE)
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
    val contractId = reference("contract_id", Contracts).references(Contracts.id)
    val hasStarted = bool("has-started").default(false)

    init {
        this.index(true, name, contractId)
    }
}

object CoopFarmers : Table() {
    val farmer = reference("farmer", Farmers, CASCADE)
    val coop = reference("coop", Coops, CASCADE)

    init {
        this.index(true, farmer, coop)
    }
}

object Contracts : IdTable<String>() {
    override val id = text("id").uniqueIndex().entityId()
    val name = text("name")
    val description = text("description")
    val egg = enumeration("egg", EggInc.Egg::class)
    val coopAllowed = bool("coop_allowed")
    val maxCoopSize = integer("max_coop_size")
    val validUntil = datetime("valid_until")
    val durationSeconds = double("duration_seconds")
}

object Goals : IntIdTable() {
    val contract = reference("contract_id", Contracts, CASCADE, NO_ACTION)
    val targetAmount = double("target_amount")
    val rewardType = enumeration("reward_type", EggInc.RewardType::class)
    val rewardSubType = text("reward_sub_type").nullable()
    val rewardAmount = double("reward_amount")
    val targetSoulEggs = double("target_soul_eggs")
}