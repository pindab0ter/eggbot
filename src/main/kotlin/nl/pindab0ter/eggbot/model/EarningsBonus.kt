package nl.pindab0ter.eggbot.model

import ch.obermuhlner.math.big.BigDecimalMath
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigDecimal.ROUND_CEILING
import java.math.MathContext
import java.math.RoundingMode

// TODO: Change into data class with calculated properties

private val BASE_SOUL_EGG_RESEARCH_BONUS = BigDecimal("0.1")
private val SOUL_EGG_RESEARCH_BONUS_PER_LEVEL = BigDecimal("0.01")
private val MAX_SOUL_EGG_RESEARCH_LEVEL = BigDecimal("140")
private val BASE_PROPHECY_EGG_RESEARCH_BONUS = BigDecimal("0.05")
private val PROPHECY_EGG_RESEARCH_BONUS_PER_LEVEL = BigDecimal("0.01")
private val MAX_PROPHECY_EGG_RESEARCH_LEVEL = BigDecimal("5")

fun earningsBonusPerSoulEgg(
    prophecyEggs: BigDecimal,
    soulEggsResearchLevel: BigDecimal = MAX_SOUL_EGG_RESEARCH_LEVEL,
    prophecyEggsResearchLevel: BigDecimal = MAX_PROPHECY_EGG_RESEARCH_LEVEL,
): BigDecimal {
    val prophecyEggBonus = ONE + BASE_PROPHECY_EGG_RESEARCH_BONUS + (PROPHECY_EGG_RESEARCH_BONUS_PER_LEVEL * prophecyEggsResearchLevel)
    val soulEggBonus: BigDecimal = BASE_SOUL_EGG_RESEARCH_BONUS + (SOUL_EGG_RESEARCH_BONUS_PER_LEVEL * soulEggsResearchLevel)

    return prophecyEggBonus.pow(prophecyEggs.intValueExact()) * soulEggBonus
}

fun earningsBonus(
    soulEggs: BigDecimal,
    prophecyEggs: BigDecimal,
    soulEggsResearchLevel: BigDecimal = MAX_SOUL_EGG_RESEARCH_LEVEL,
    prophecyEggsResearchLevel: BigDecimal = MAX_PROPHECY_EGG_RESEARCH_LEVEL,
) = soulEggs * earningsBonusPerSoulEgg(prophecyEggs, soulEggsResearchLevel, prophecyEggsResearchLevel)

fun earningsBonusForNextRank(
    soulEggs: BigDecimal,
    prophecyEggs: BigDecimal,
    soulEggsResearchLevel: BigDecimal = MAX_SOUL_EGG_RESEARCH_LEVEL,
    prophecyEggsResearchLevel: BigDecimal = MAX_PROPHECY_EGG_RESEARCH_LEVEL,
) = BigDecimal("1".padEnd(
    earningsBonus(
        soulEggs,
        prophecyEggs,
        soulEggsResearchLevel,
        prophecyEggsResearchLevel
    ).setScale(0, RoundingMode.FLOOR).toPlainString().length + 1, '0')
)

fun earningsBonusForNextRank(
    earningsBonus: BigDecimal,
): BigDecimal = BigDecimal("1".padEnd(earningsBonus.setScale(0, RoundingMode.FLOOR).toPlainString().length + 1, '0'))

fun soulEggsForNextRank(
    soulEggs: BigDecimal,
    prophecyEggs: BigDecimal,
    soulEggsResearchLevel: BigDecimal = MAX_SOUL_EGG_RESEARCH_LEVEL,
    prophecyEggsResearchLevel: BigDecimal = MAX_PROPHECY_EGG_RESEARCH_LEVEL,
): BigDecimal = earningsBonusForNextRank(soulEggs, prophecyEggs, soulEggsResearchLevel, prophecyEggsResearchLevel)
    .divide(earningsBonusPerSoulEgg(prophecyEggs, soulEggsResearchLevel, prophecyEggsResearchLevel), ROUND_CEILING)

fun soulEggsToNextRank(
    soulEggs: BigDecimal,
    prophecyEggs: BigDecimal,
    soulEggsResearchLevel: BigDecimal = MAX_SOUL_EGG_RESEARCH_LEVEL,
    prophecyEggsResearchLevel: BigDecimal = MAX_PROPHECY_EGG_RESEARCH_LEVEL,
): BigDecimal = soulEggsForNextRank(soulEggs, prophecyEggs, soulEggsResearchLevel, prophecyEggsResearchLevel) - soulEggs

fun prophecyEggsForNextRank(
    soulEggs: BigDecimal,
    prophecyEggs: BigDecimal,
    soulEggsResearchLevel: BigDecimal = MAX_SOUL_EGG_RESEARCH_LEVEL,
    prophecyEggsResearchLevel: BigDecimal = MAX_PROPHECY_EGG_RESEARCH_LEVEL,
): BigDecimal =
    prophecyEggsToNextRank(soulEggs, prophecyEggs, soulEggsResearchLevel, prophecyEggsResearchLevel) + prophecyEggs

fun prophecyEggsToNextRank(
    soulEggs: BigDecimal,
    prophecyEggs: BigDecimal,
    soulEggsResearchLevel: BigDecimal = MAX_SOUL_EGG_RESEARCH_LEVEL,
    prophecyEggsResearchLevel: BigDecimal = MAX_PROPHECY_EGG_RESEARCH_LEVEL,
): BigDecimal =
        BigDecimalMath.log(earningsBonusForNextRank(soulEggs, prophecyEggs, soulEggsResearchLevel,
            prophecyEggsResearchLevel)
            .divide(earningsBonus(soulEggs, prophecyEggs, soulEggsResearchLevel, prophecyEggsResearchLevel),
                MathContext.DECIMAL128),
            MathContext.DECIMAL128).divide(BigDecimalMath.log(
            ONE + BASE_PROPHECY_EGG_RESEARCH_BONUS + (PROPHECY_EGG_RESEARCH_BONUS_PER_LEVEL * prophecyEggsResearchLevel),
            MathContext.DECIMAL128), MathContext.DECIMAL128).setScale(0, ROUND_CEILING)
