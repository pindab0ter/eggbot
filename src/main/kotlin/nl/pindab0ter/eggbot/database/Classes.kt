package nl.pindab0ter.eggbot.database

import com.auxbrain.ei.EggInc
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.network.AuxBrain
import nl.pindab0ter.eggbot.utilities.prophecyBonus
import nl.pindab0ter.eggbot.utilities.soulBonus
import nl.pindab0ter.eggbot.utilities.sumBy
import nl.pindab0ter.eggbot.utilities.toDateTime
import org.jetbrains.exposed.dao.*
import java.math.BigDecimal
import java.math.BigDecimal.*

class DiscordUser(id: EntityID<String>) : Entity<String>(id) {
    val discordId: String get() = id.value
    var discordTag by DiscordUsers.discordTag
    var inactiveUntil by DiscordUsers.inactiveUntil
    val farmers by Farmer referrersOn Farmers.discordId

    val isActive: Boolean get() = inactiveUntil?.isBeforeNow ?: true

    fun updateTag() = EggBot.guild.getMemberById(discordId)?.user?.asTag.takeIf { it != discordTag }?.let { tag ->
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

    val role: Role? get() = roles.find { earningsBonus in it.range }

    val nextRole: Role? get() = roles.getOrNull(roles.indexOf(role) + 1)

    private val roles = listOf(
        Role(TEN.pow(0), TEN.pow(3) - ONE, "Farmer"),
        Role(TEN.pow(3), TEN.pow(4) - ONE, "Farmer 2"),
        Role(TEN.pow(4), TEN.pow(5) - ONE, "Farmer 3"),
        Role(TEN.pow(5), TEN.pow(6) - ONE, "Kilofarmer"),
        Role(TEN.pow(6), TEN.pow(7) - ONE, "Kilofarmer 2"),
        Role(TEN.pow(7), TEN.pow(8) - ONE, "Kilofarmer 3"),
        Role(TEN.pow(8), TEN.pow(9) - ONE, "Megafarmer"),
        Role(TEN.pow(9), TEN.pow(10) - ONE, "Megafarmer 2"),
        Role(TEN.pow(10), TEN.pow(11) - ONE, "Megafarmer 3"),
        Role(TEN.pow(11), TEN.pow(12) - ONE, "Gigafarmer"),
        Role(TEN.pow(12), TEN.pow(13) - ONE, "Gigafarmer 2"),
        Role(TEN.pow(13), TEN.pow(14) - ONE, "Gigafarmer 3"),
        Role(TEN.pow(14), TEN.pow(15) - ONE, "Terafarmer"),
        Role(TEN.pow(15), TEN.pow(16) - ONE, "Terafarmer 2"),
        Role(TEN.pow(16), TEN.pow(17) - ONE, "Terafarmer 3"),
        Role(TEN.pow(17), TEN.pow(18) - ONE, "Petafarmer"),
        Role(TEN.pow(18), TEN.pow(19) - ONE, "Petafarmer 2"),
        Role(TEN.pow(19), TEN.pow(20) - ONE, "Petafarmer 3"),
        Role(TEN.pow(20), TEN.pow(21) - ONE, "Exafarmer"),
        Role(TEN.pow(21), TEN.pow(22) - ONE, "Exafarmer 2"),
        Role(TEN.pow(22), TEN.pow(23) - ONE, "Exafarmer 3"),
        Role(TEN.pow(23), TEN.pow(24) - ONE, "Zettafarmer"),
        Role(TEN.pow(24), TEN.pow(25) - ONE, "Zettafarmer 2"),
        Role(TEN.pow(25), TEN.pow(26) - ONE, "Zettafarmer 3"),
        Role(TEN.pow(26), TEN.pow(27) - ONE, "Yodafarmer"),
        Role(TEN.pow(27), TEN.pow(28) - ONE, "Yodafarmer 2"),
        Role(TEN.pow(28), TEN.pow(29) - ONE, "Yodafarmer 3")
    )

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
        if (!backup.hasGame()) return
        inGameName = backup.userName
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

    data class Role(
        val lowerBound: BigDecimal,
        val upperBound: BigDecimal,
        val name: String
    ) {
        val range: ClosedRange<BigDecimal> = lowerBound..upperBound
    }

    companion object : EntityClass<String, Farmer>(Farmers)
}

class Coop(id: EntityID<Int>) : IntEntity(id) {
    var name by Coops.name
    var contract by Coops.contract

    var farmers by Farmer via CoopFarmers

    val earningsBonus: BigDecimal get() = farmers.sumBy { it.earningsBonus }
    val activeEarningsBonus: BigDecimal get() = farmers.sumBy { it.activeEarningsBonus }

    companion object : IntEntityClass<Coop>(Coops)
}
