package nl.pindab0ter.eggbot.model

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.shouldBe
import nl.pindab0ter.eggbot.helpers.plusOrMinus
import java.math.BigDecimal

class EarningsBonusTest : DescribeSpec({
    val subject = EarningsBonus(soulEggs = BigDecimal("501194838109874240"), prophecyEggs = BigDecimal("113"))

    describe("Earnings bonus per soul egg") {
        subject.earningsBonusPerSoulEgg shouldBe (BigDecimal("7136161.694621817") plusOrMinus BigDecimal("1E-8"))
    }

    describe("Earnings bonus") {
        subject.earningsBonus shouldBe (BigDecimal("3576607405261865562931300") plusOrMinus BigDecimal("1E10"))
    }

    describe("Earnings bonus for next rank") {
        subject.earningsBonusForNextRank shouldBeEqualComparingTo BigDecimal("1E25")
    }

    describe("Soul Eggs for next rank") {
        subject.soulEggsForNextRank shouldBe (BigDecimal("1401313539116753664") plusOrMinus BigDecimal("1E10"))
    }

    describe("Soul Eggs to next rank") {
        subject.soulEggsToNextRank shouldBe (BigDecimal("900118701006879600") plusOrMinus BigDecimal("1E10"))
    }

    describe("Prophecy Eggs for next rank") {
        subject.prophecyEggsForNextRank shouldBe BigDecimal("124")
    }

    describe("Prophecy Eggs to next rank") {
        subject.prophecyEggsToNextRank shouldBe BigDecimal("11")
    }
})
