package nl.pindab0ter.eggbot.database

import com.auxbrain.ei.EggInc
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
    val discordId: String get() = id.value
    var discordTag by DiscordUsers.discordTag
    var inactiveUntil by DiscordUsers.inactiveUntil
    val farmers by Farmer referrersOn Farmers.discordId

    val isActive: Boolean get() = inactiveUntil?.isBeforeNow ?: true

    companion object : EntityClass<String, DiscordUser>(DiscordUsers)
}

class Farmer(id: EntityID<String>) : Entity<String>(id) {
    val inGameId: String get() = id.value
    var discordUser by DiscordUser referencedOn Farmers.discordId
    var inGameName by Farmers.inGameName

    @Suppress("PrivatePropertyName")
    private var soulEggs_ by Farmers.soulEggs
    var soulEggs
        get() = if (inGameId == "G:1310382811") 39505097770735L else soulEggs_
        set(value) {
            soulEggs_ = value
        }
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

    // @formatter:off
    private val roles = listOf(
        Role(BigInteger("0"),                    BigInteger("999"),                   "Farmer"),
        Role(BigInteger("1000"),                 BigInteger("9999"),                  "Farmer 2"),
        Role(BigInteger("10000"),                BigInteger("99999"),                 "Farmer 3"),
        Role(BigInteger("100000"),               BigInteger("999999"),                "Kilofarmer"),
        Role(BigInteger("1000000"),              BigInteger("9999999"),               "Kilofarmer 2"),
        Role(BigInteger("10000000"),             BigInteger("99999999"),              "Kilofarmer 3"),
        Role(BigInteger("100000000"),            BigInteger("999999999"),             "Megafarmer"),
        Role(BigInteger("1000000000"),           BigInteger("9999999999"),            "Megafarmer 2"),
        Role(BigInteger("10000000000"),          BigInteger("99999999999"),           "Megafarmer 3"),
        Role(BigInteger("100000000000"),         BigInteger("999999999999"),          "Gigafarmer"),
        Role(BigInteger("1000000000000"),        BigInteger("9999999999999"),         "Gigafarmer 2"),
        Role(BigInteger("10000000000000"),       BigInteger("99999999999999"),        "Gigafarmer 3"),
        Role(BigInteger("100000000000000"),      BigInteger("999999999999999"),       "Terafarmer"),
        Role(BigInteger("1000000000000000"),     BigInteger("9999999999999999"),      "Terafarmer 2"),
        Role(BigInteger("10000000000000000"),    BigInteger("99999999999999999"),     "Terafarmer 3"),
        Role(BigInteger("100000000000000000"),   BigInteger("999999999999999999"),    "Petafarmer"),
        Role(BigInteger("1000000000000000000"),  BigInteger("9999999999999999999"),   "Petafarmer 2"),
        Role(BigInteger("10000000000000000000"), BigInteger("99999999999999999999"),  "Petafarmer 3")
    )
    // @formatter:on

    val bonusPerSoulEgg: BigInteger
        get() {
            val soulEggBonus = 10 + soulBonus
            val prophecyEggBonus = (1.05 + 0.01 * prophecyBonus)
            return BigDecimal(prophecyEggBonus.pow(prophecyEggs.toInt()) * soulEggBonus).toBigInteger()
        }
    val earningsBonus: BigInteger
        get() {
            return (BigDecimal(soulEggs) * BigDecimal(bonusPerSoulEgg)).toBigInteger()
        }
    val activeEarningsBonus: BigInteger get() = if (isActive) earningsBonus else BigInteger.ZERO

    fun update() = AuxBrain.getFarmerBackup(inGameId).let { (backup, _) ->
        if (backup == null || !backup.hasData()) return@let
        update(backup)
    }

    fun update(backup: EggInc.Backup) = transaction {
        if (!backup.hasData()) return@transaction
        soulEggs = backup.data.soulEggs
        prophecyEggs = backup.data.prophecyEggs
        soulBonus = backup.data.soulBonus
        prophecyBonus = backup.data.prophecyBonus
        prestiges = backup.stats.prestigeCount
        droneTakedowns = backup.stats.droneTakedowns
        eliteDroneTakedowns = backup.stats.droneTakedownsElite
        lastUpdated = DateTime.now()
    }

    data class Role(
        val lowerBound: BigInteger,
        val upperBound: BigInteger,
        val name: String
    ) {
        val range: ClosedRange<BigInteger> = lowerBound..upperBound
    }

    companion object : EntityClass<String, Farmer>(Farmers)
}

class Coop(id: EntityID<Int>) : IntEntity(id) {
    var name by Coops.name
    var contract by Contract referencedOn Coops.contract
    var hasStarted by Coops.hasStarted

    var farmers by Farmer via CoopFarmers

    val earningsBonus: BigInteger get() = farmers.sumBy { it.earningsBonus }
    val activeEarningsBonus: BigInteger get() = farmers.sumBy { it.activeEarningsBonus }

    companion object : IntEntityClass<Coop>(Coops)
}

class Contract(id: EntityID<String>) : Entity<String>(id) {
    val identifier: String get() = id.value

    var name by Contracts.name
    var description by Contracts.description
    var egg by Contracts.egg
    var coopAllowed by Contracts.coopAllowed
    var maxCoopSize by Contracts.maxCoopSize
    var validUntil by Contracts.validUntil
    var durationSeconds by Contracts.durationSeconds
    val coops by Coop referrersOn Coops.contract
    val goals by Goal referrersOn Goals.contract

    val finalAmount get() = goals.sortedByDescending { it.targetAmount }.first().targetAmount

    companion object : EntityClass<String, Contract>(Contracts) {
        fun new(contractInfo: EggInc.Contract): Contract = super.new(contractInfo.identifier) {
            this.name = contractInfo.name
            this.description = contractInfo.description
            this.egg = contractInfo.egg
            this.coopAllowed = contractInfo.coopAllowed == 1
            this.maxCoopSize = contractInfo.maxCoopSize
            this.validUntil = DateTime(contractInfo.expirationTime.toLong())
            this.durationSeconds = contractInfo.lengthSeconds
        }.also { contract ->
            contractInfo.goalsList.forEach { goalInfo -> Goal.new(contract, goalInfo) }
        }

        fun getOrNew(contract: EggInc.Contract): Contract =
            super.findById(contract.identifier) ?: new(contract)

        fun getOrNew(contractId: String): Contract? =
            super.findById(contractId) ?: AuxBrain.getContracts().contractsList.find {
                it.identifier == contractId
            }?.let { new(it) }
    }
}

class Goal(id: EntityID<Int>) : IntEntity(id) {
    var contract by Contract referencedOn Goals.contract
    var targetAmount by Goals.targetAmount
    var rewardType by Goals.rewardType
    var rewardSubType by Goals.rewardSubType
    var rewardAmount by Goals.rewardAmount
    var targetSoulEggs by Goals.targetSoulEggs

    companion object : IntEntityClass<Goal>(Goals) {
        fun new(contract: Contract, goal: EggInc.Goal) = super.new {
            this.contract = contract
            this.targetAmount = goal.targetAmount
            this.rewardType = goal.rewardType
            this.rewardSubType = goal.rewardSubType
            this.rewardAmount = goal.rewardAmount
            this.targetSoulEggs = goal.targetSoulEggs
        }
    }
}
