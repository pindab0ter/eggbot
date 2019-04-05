package nl.pindab0ter.eggbot.database

import org.jetbrains.exposed.dao.*
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.pow

class DiscordUser(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, DiscordUser>(DiscordUsers)

    val discordId: String get() = id.value
    var discordTag by DiscordUsers.discordTag
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
    var coops by Coop via CoopFarmers

    val earningsBonus: BigInteger
        get() {
            val soulEggBonus = 10 + soulBonus
            val prophecyEggBonus = (1.05 + 0.01 * prophecyBonus)
            val bonusPerSoulEgg = prophecyEggBonus.pow(prophecyEggs.toInt()) * soulEggBonus
            return (BigDecimal(soulEggs) * BigDecimal(bonusPerSoulEgg)).toBigInteger()
        }
}

class Coop(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<Coop>(Coops)

    var name by Coops.name
    var contractId by Coops.contractId
    var hasStarted by Coops.hasStarted
    var farmers by Farmer via CoopFarmers
}
