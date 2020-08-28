package nl.pindab0ter.eggbot.model

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import nl.pindab0ter.eggbot.helpers.plusOrMinus
import java.math.BigDecimal

class EarningsBonusTest : DescribeSpec({
    val subject = EarningsBonus(soulEggs = BigDecimal("501194838109874240"), prophecyEggs = BigDecimal("113"))

    describe("Earnings bonus per soul egg") {
        subject.earningsBonusPerSoulEgg shouldBe (BigDecimal("71361.61694621817") plusOrMinus BigDecimal("0.0000000001"))
    }

    describe("Earnings bonus") {
        subject.earningsBonus shouldBe (BigDecimal("35766074052618655629313") plusOrMinus BigDecimal("10000000"))
    }

    describe("Earnings Bonus for next rank") {
        subject.earningsBonusForNextRank shouldBe BigDecimal("100000000000000000000000")
    }

    describe("Soul Eggs for next rank") {
        subject.soulEggsForNextRank shouldBe (BigDecimal("1401313539116753664") plusOrMinus BigDecimal("10000000"))
    }

    describe("Soul Eggs to next rank") {
        subject.soulEggsToNextRank shouldBe (BigDecimal("900118701006879600") plusOrMinus BigDecimal("10000000"))
    }

    describe("Prophecy Eggs for next rank") {
        subject.prophecyEggsForNextRank shouldBe BigDecimal("124")
    }

    describe("Prophecy Eggs to next rank") {
        subject.prophecyEggsToNextRank shouldBe BigDecimal("11")
    }
})
