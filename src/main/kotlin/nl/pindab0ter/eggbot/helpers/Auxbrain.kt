package nl.pindab0ter.eggbot.helpers

import ch.obermuhlner.math.big.BigDecimalMath
import com.auxbrain.ei.*
import com.kotlindiscord.kord.extensions.utils.capitalizeWords
import nl.pindab0ter.eggbot.*
import org.joda.time.DateTime.now
import org.joda.time.Duration
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigDecimal.ZERO
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

// Basic information
val Backup.timeSinceBackup: Duration
    get() = Duration(approxTime.toDateTime(), now())
val CoopStatus.FarmInfo.timeSinceLastCheckIn: Duration
    get() = Duration.standardSeconds(timestamp.roundToLong().absoluteValue)

// Farms
fun Backup.farmFor(contractId: String): Backup.Farm? = farms.firstOrNull { farm ->
    farm.contractId == contractId
}

val Backup.Farm.timeSinceLastStep: Duration
    get() = Duration(lastStepTime.toDateTime(), now())

// Earnings bonus
val Backup.Game.soulEggResearchLevel: BigDecimal
    get() = epicResearch.find { it.id == "soul_eggs" }?.level?.toBigDecimal() ?: ZERO
val Backup.Game.soulEggBonus: BigDecimal
    get() = BASE_SOUL_EGG_RESEARCH_BONUS + (SOUL_EGG_RESEARCH_BONUS_PER_LEVEL * soulEggResearchLevel)
val Backup.Game.prophecyEggResearchLevel: BigDecimal
    get() = epicResearch.find { it.id == "prophecy_bonus" }?.level?.toBigDecimal() ?: ZERO
val Backup.Game.prophecyEggBonus: BigDecimal
    get() = ONE + BASE_PROPHECY_EGG_RESEARCH_BONUS + (PROPHECY_EGG_RESEARCH_BONUS_PER_LEVEL * prophecyEggResearchLevel)
val Backup.Game.earningsBonusPerSoulEgg: BigDecimal
    get() = prophecyEggBonus.pow(prophecyEggs.toInt()) * soulEggBonus * BigDecimal("100")
val Backup.Game.earningsBonus: BigDecimal
    get() = soulEggs.toBigDecimal() * earningsBonusPerSoulEgg

// Requirements for next rank
val Backup.Game.earningsBonusForNextRank: BigDecimal
    get() = BigDecimal("1".padEnd(length = earningsBonus.floor().toPlainString().length + 1, padChar = '0'))
val Backup.Game.soulEggsForNextRank: BigDecimal
    get() = earningsBonusForNextRank.divide(earningsBonusPerSoulEgg, RoundingMode.CEILING)
val Backup.Game.soulEggsToNextRank: BigDecimal
    get() = soulEggsForNextRank - soulEggs.toBigDecimal()
val Backup.Game.prophecyEggsToNextRank: BigDecimal
    get() = BigDecimalMath.log(earningsBonusForNextRank.divide(earningsBonus, MathContext.DECIMAL128), MathContext.DECIMAL128)
        .divide(BigDecimalMath.log(prophecyEggBonus, MathContext.DECIMAL128), MathContext.DECIMAL128).ceiling()

// Contracts
val Backup.localContracts: List<LocalContract>
    get() = contracts.contracts + contracts.archived

enum class AttemptStatus { NEVER_ATTEMPTED, FAILED_TO_GET_PROPHECY_EGG, FAILED_TO_COMPLETE_ALL_GOALS, COMPLETED }

fun Backup.attemptStatusFor(contractId: String): AttemptStatus {
    val localContract = this.localContracts.firstOrNull { it.contract.id == contractId }

    return when {
        localContract == null -> AttemptStatus.NEVER_ATTEMPTED

        localContract.contract.indexOfPeGoal != null && (localContract.goalsAchieved ?: 0) < ((localContract.contract.indexOfPeGoal ?: 0) + 1) ->
            AttemptStatus.FAILED_TO_GET_PROPHECY_EGG

        (localContract.goalsAchieved ?: 0) < localContract.contract.goals.size ->
            AttemptStatus.FAILED_TO_COMPLETE_ALL_GOALS

        else -> AttemptStatus.COMPLETED
    }
}

val Backup.inGameName: String
    get() = userName.ifBlank { NO_ALIAS }
val Backup.rocketsLaunched: Int
    get() = artifactsDatabase.currentMissions.count { mission ->
        mission.status !== Mission.Status.FUELING && mission.status !== Mission.Status.PREPARE_TO_LAUNCH
    } + artifactsDatabase.missionArchive.count { mission ->
        mission.status !== Mission.Status.FUELING && mission.status !== Mission.Status.PREPARE_TO_LAUNCH
    }
val Contract.finalGoal: BigDecimal
    get() = BigDecimal(goals.maxByOrNull { it.targetAmount }?.targetAmount ?: 0.0)
val Contract.indexOfPeGoal: Int?
    get() = goals.indexOfFirst { it.rewardType == RewardType.PROPHECY_EGGS }.takeIf { it != -1 }
val LocalContract.finished: Boolean
    get() = goalsAchieved == contract.goals.size
val LocalContract.timeRemaining: Duration
    get() = contract.lengthSeconds.toDuration().minus(Duration(timeAccepted.toDateTime(), now()))
val CoopStatus.eggsLaid: BigDecimal
    get() = BigDecimal(totalAmount)
val CoopStatus.timeRemaining: Duration
    get() = secondsRemaining.toDuration()

// Eggs
val Egg.displayName: String
    get() = name.replace("_", " ").lowercase().capitalizeWords()