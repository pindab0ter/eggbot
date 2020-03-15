package nl.pindab0ter.eggbot.database

import com.auxbrain.ei.EggInc
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.EggBot.clientVersion
import nl.pindab0ter.eggbot.EggBot.guild
import nl.pindab0ter.eggbot.network.AuxBrain
import nl.pindab0ter.eggbot.utilities.*
import org.jetbrains.exposed.dao.*
import java.math.BigDecimal
import java.math.BigDecimal.*

class DiscordUser(id: EntityID<String>) : Entity<String>(id) {
    val discordId: String get() = id.value
    var discordTag by DiscordUsers.discordTag
    val discordName: String get() = discordTag.substring(0, discordTag.length - 5)
    var inactiveUntil by DiscordUsers.inactiveUntil
    val farmers by Farmer referrersOn Farmers.discordId

    val isActive: Boolean get() = inactiveUntil?.isBeforeNow ?: true

    fun updateTag() = guild.getMemberById(discordId)?.user?.asTag.takeIf { it != discordTag }?.let { tag ->
        discordTag = tag
    }

    companion object : EntityClass<String, DiscordUser>(DiscordUsers)
}

class Farmer(id: EntityID<String>) : Entity<String>(id) {
    val inGameId: String get() = id.value
    var discordUser by DiscordUser referencedOn Farmers.discordId
    var inGameName by Farmers.inGameName

    internal var soulEggsLong by Farmers.soulEggsLong
    internal var soulEggsDouble by Farmers.soulEggsDouble
    val soulEggs: BigDecimal
        get() = if (soulEggsLong > 0) BigDecimal(soulEggsLong) else BigDecimal(soulEggsDouble)
    var prophecyEggs by Farmers.prophecyEggs
    var soulBonus by Farmers.soulBonus
    var prophecyBonus by Farmers.prophecyBonus
    var prestiges by Farmers.prestiges
    var droneTakedowns by Farmers.droneTakedowns
    var eliteDroneTakedowns by Farmers.eliteDroneTakedowns
    var lastUpdated by Farmers.lastUpdated
    var coops by Coop via CoopFarmers

    val isActive: Boolean get() = discordUser.isActive

    val role: String get() = earningsBonus.asFarmerRole()

    val bonusPerSoulEgg: BigDecimal
        get() {
            val soulEggBonus = BigDecimal(10 + soulBonus)
            val prophecyEggBonus = BigDecimal(1.05) + BigDecimal(0.01) * BigDecimal(prophecyBonus)
            return prophecyEggBonus.pow(prophecyEggs.toInt()) * soulEggBonus
        }
    val earningsBonus: BigDecimal get() = soulEggs * bonusPerSoulEgg
    val activeEarningsBonus: BigDecimal get() = if (isActive) earningsBonus else ZERO

    fun update() = AuxBrain.getFarmerBackup(inGameId).let { backup ->
        if (backup == null || !backup.hasGame()) return@let
        update(backup)
    }

    fun update(backup: EggInc.Backup) {
        if (backup.clientVersion > clientVersion) clientVersion = backup.clientVersion
        if (!backup.hasGame()) return
        if (!backup.userName.matches(Regex("""\[(android-)?unknown\]"""))) inGameName = backup.userName
        prestiges = backup.stats.prestigeCount
        soulEggsLong = backup.game.soulEggsLong
        soulEggsDouble = backup.game.soulEggsDouble
        prophecyEggs = backup.game.prophecyEggs
        soulBonus = backup.game.soulBonus
        prophecyBonus = backup.game.prophecyBonus
        droneTakedowns = backup.stats.droneTakedowns
        eliteDroneTakedowns = backup.stats.droneTakedownsElite
        lastUpdated = backup.approxTime.toDateTime()
    }

    companion object : EntityClass<String, Farmer>(Farmers)
}

class Coop(id: EntityID<Int>) : IntEntity(id) {
    var name by Coops.name
    var contract by Coops.contract
    var roleId by Coops.role_id

    var farmers by Farmer via CoopFarmers

    val earningsBonus: BigDecimal get() = farmers.sumByBigDecimal { it.earningsBonus }
    val activeEarningsBonus: BigDecimal get() = farmers.sumByBigDecimal { it.activeEarningsBonus }

    companion object : IntEntityClass<Coop>(Coops)
}
