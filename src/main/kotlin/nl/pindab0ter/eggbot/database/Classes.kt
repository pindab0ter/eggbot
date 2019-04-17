package nl.pindab0ter.eggbot.database

import nl.pindab0ter.eggbot.network.AuxBrain
import nl.pindab0ter.eggbot.prophecyBonus
import nl.pindab0ter.eggbot.soulBonus
import nl.pindab0ter.eggbot.sumBy
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.pow

class DiscordUser(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, DiscordUser>(DiscordUsers)

    val discordId: String get() = id.value
    var discordTag by DiscordUsers.discordTag
    var isActive by DiscordUsers.isActive
    val farmers by Farmer referrersOn Farmers.discordId
}

class Farmer(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, Farmer>(Farmers)

    val inGameId: String get() = id.value
    var discordId by DiscordUser referencedOn Farmers.discordId
    var inGameName by Farmers.inGameName
    var soulEggs by Farmers.soulEggs
    var prophecyEggs by Farmers.prophecyEggs
    var soulBonus by Farmers.soulBonus
    var prophecyBonus by Farmers.prophecyBonus
    var lastUpdated by Farmers.lastUpdated
    var coops by Coop via CoopFarmers

    val isActive: Boolean
        get() = discordUser.isActive

    val earningsBonus: BigInteger
        get() {
            val soulEggBonus = 10 + soulBonus
            val prophecyEggBonus = (1.05 + 0.01 * prophecyBonus)
            val bonusPerSoulEgg = prophecyEggBonus.pow(prophecyEggs.toInt()) * soulEggBonus
            return (BigDecimal(soulEggs) * BigDecimal(bonusPerSoulEgg)).toBigInteger()
        }

    val activeEarningsBonus: BigInteger
        get() = if (isActive) earningsBonus else BigInteger.ZERO

    fun update() = AuxBrain.getFarmerBackup(inGameId).let { (backup, _) ->
        if (backup == null) return@let
        transaction {
            soulEggs = backup.data.soulEggs
            prophecyEggs = backup.data.prophecyEggs
            soulBonus = backup.data.soulBonus
            prophecyBonus = backup.data.prophecyBonus
            lastUpdated = DateTime.now()
        }
    }
}

class Coop(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Coop>(Coops)

    var name by Coops.name
    var contractId by Coops.contractId
    var hasStarted by Coops.hasStarted
    var farmers by Farmer via CoopFarmers

    val earningsBonus: BigInteger
        get() = farmers.sumBy { it.earningsBonus }
    val activeEarningsBonus: BigInteger
        get() = farmers.sumBy { it.activeEarningsBonus }
}
