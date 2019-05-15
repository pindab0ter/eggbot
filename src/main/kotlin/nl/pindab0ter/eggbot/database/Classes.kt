package nl.pindab0ter.eggbot.database

import com.auxbrain.ei.EggInc
import nl.pindab0ter.eggbot.*
import nl.pindab0ter.eggbot.network.AuxBrain
import org.jetbrains.exposed.dao.*
import org.joda.time.DateTime
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

    // @formatter:off
    @Suppress("PrivatePropertyName")
    private var soulEggs_ by Farmers.soulEggs
    var soulEggs get() = when (inGameId) {
            "G:1310382811"          -> 39505097770735L
            "104311114077828861120" ->  2500000000000L
            else -> soulEggs_
        }
        set(value) {
            soulEggs_ = value
        }
    // @formatter:on

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
            val prophecyEggBonus = BigDecimal(1.05 + 0.01 * prophecyBonus)
            return prophecyEggBonus.pow(prophecyEggs.toInt()) * soulEggBonus
        }
    val earningsBonus: BigDecimal get() = BigDecimal(soulEggs) * bonusPerSoulEgg
    val activeEarningsBonus: BigDecimal get() = if (isActive) earningsBonus else ZERO

    fun update() = AuxBrain.getFarmerBackup(inGameId).let { (backup, _) ->
        if (backup == null || !backup.hasData()) return@let
        update(backup)
    }

    fun update(backup: EggInc.Backup) {
        if (!backup.hasData()) return
        soulEggs = backup.data.soulEggs
        prophecyEggs = backup.data.prophecyEggs
        soulBonus = backup.data.soulBonus
        prophecyBonus = backup.data.prophecyBonus
        prestiges = backup.stats.prestigeCount
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
    var contract by Contract referencedOn Coops.contract
    var hasStarted by Coops.hasStarted

    var farmers by Farmer via CoopFarmers

    val earningsBonus: BigDecimal get() = farmers.sumBy { it.earningsBonus }
    val activeEarningsBonus: BigDecimal get() = farmers.sumBy { it.activeEarningsBonus }

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
