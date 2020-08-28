package nl.pindab0ter.eggbot.model

import ch.obermuhlner.math.big.BigDecimalMath.log
import nl.pindab0ter.eggbot.helpers.ceiling
import nl.pindab0ter.eggbot.model.database.Farmer
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.MathContext.DECIMAL128
import java.math.RoundingMode.CEILING
import java.math.RoundingMode.FLOOR


data class EarningsBonus(
    val soulEggs: BigDecimal,
    val prophecyEggs: BigDecimal,
    val soulEggsResearchLevel: BigDecimal = MAX_SOUL_EGG_RESEARCH_LEVEL,
    val prophecyEggsResearchLevel: BigDecimal = MAX_PROPHECY_EGG_RESEARCH_LEVEL,
) {
    constructor(farmer: Farmer) : this(
        farmer.soulEggs,
        farmer.prophecyEggs.toBigDecimal(),
        farmer.soulEggResearchLevel.toBigDecimal(),
        farmer.prophecyEggResearchLevel.toBigDecimal()
    )

    val prophecyEggBonus: BigDecimal =
        ONE + BASE_PROPHECY_EGG_RESEARCH_BONUS + (PROPHECY_EGG_RESEARCH_BONUS_PER_LEVEL * prophecyEggsResearchLevel)

    val soulEggBonus: BigDecimal =
        BASE_SOUL_EGG_RESEARCH_BONUS + (SOUL_EGG_RESEARCH_BONUS_PER_LEVEL * soulEggsResearchLevel)

    val earningsBonusPerSoulEgg: BigDecimal =
        prophecyEggBonus.pow(prophecyEggs.toInt()) * soulEggBonus * BigDecimal("100")

    val earningsBonus: BigDecimal =
        soulEggs * earningsBonusPerSoulEgg

    val earningsBonusForNextRank: BigDecimal =
        BigDecimal("1".padEnd(length = earningsBonus.setScale(0, FLOOR).toPlainString().length + 1, padChar = '0'))

    val soulEggsForNextRank: BigDecimal =
        earningsBonusForNextRank.divide(earningsBonusPerSoulEgg, CEILING)

    val soulEggsToNextRank: BigDecimal =
        soulEggsForNextRank - soulEggs

    val prophecyEggsToNextRank: BigDecimal =
        log(earningsBonusForNextRank.divide(earningsBonus, DECIMAL128), DECIMAL128)
            .divide(log(prophecyEggBonus, DECIMAL128), DECIMAL128).ceiling()

    val prophecyEggsForNextRank: BigDecimal =
        prophecyEggsToNextRank + prophecyEggs

    companion object {
        private val BASE_SOUL_EGG_RESEARCH_BONUS = BigDecimal("0.1")
        private val SOUL_EGG_RESEARCH_BONUS_PER_LEVEL = BigDecimal("0.01")
        val MAX_SOUL_EGG_RESEARCH_LEVEL = BigDecimal("140")
        private val BASE_PROPHECY_EGG_RESEARCH_BONUS = BigDecimal("0.05")
        private val PROPHECY_EGG_RESEARCH_BONUS_PER_LEVEL = BigDecimal("0.01")
        val MAX_PROPHECY_EGG_RESEARCH_LEVEL = BigDecimal("5")
    }
}
