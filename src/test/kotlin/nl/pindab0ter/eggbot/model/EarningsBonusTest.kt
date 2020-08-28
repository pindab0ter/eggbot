package nl.pindab0ter.eggbot.model

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import nl.pindab0ter.eggbot.helpers.plusOrMinus
import java.math.BigDecimal

class EarningsBonusTest : DescribeSpec({
    val soulEggs = BigDecimal("501194838109874240")
    val prophecyEggs = BigDecimal("113")

    describe("Earnings bonus per soul egg") {
        context("Maxed out research") {
            earningsBonusPerSoulEgg(prophecyEggs) shouldBe
                    (BigDecimal("71361.61694621817") plusOrMinus BigDecimal("0.0000000001"))
        }
    }

    describe("Earnings bonus") {
        context("Maxed out research") {
            earningsBonus(soulEggs, prophecyEggs) shouldBe
                    (BigDecimal("35766074052618655629313") plusOrMinus BigDecimal("10000000"))
        }
    }

    describe("Earnings Bonus for next rank") {
        context("Maxed out research") {
            earningsBonusForNextRank(earningsBonus(soulEggs, prophecyEggs)) shouldBe
                    BigDecimal("100000000000000000000000")
            earningsBonusForNextRank(soulEggs, prophecyEggs) shouldBe
                    BigDecimal("100000000000000000000000")
        }
    }

    describe("Soul Eggs for next rank") {
        // TODO: Change scenarios to more reflect actual usage scenarios; From a rank to a rank
        // TODO: Use partial application to pre-fill functions

        xcontext("No eggs and no research") {
        }

        xcontext("One level of Soul Egg research") {
        }

        xcontext("Seventy levels of Soul Egg research") {
        }

        xcontext("Two levels of Prophecy Egg research") {
        }

        context("Maxed out research") {
            soulEggsForNextRank(soulEggs, prophecyEggs) shouldBe
                    (BigDecimal("1401313539116753664") plusOrMinus BigDecimal("10000000"))
        }
    }

    describe("Soul Eggs to next rank") {
        context("Maxed out research") {
            soulEggsToNextRank(soulEggs, prophecyEggs) shouldBe
                    (BigDecimal("900118701006879600") plusOrMinus BigDecimal("10000000"))
        }
    }

    describe("Prophecy Eggs for next rank") {
        context("Maxed out research") {
            prophecyEggsForNextRank(soulEggs, prophecyEggs) shouldBe BigDecimal("124")
        }
    }

    describe("Prophecy Eggs to next rank") {
        context("Maxed out research") {
            prophecyEggsToNextRank(soulEggs, prophecyEggs) shouldBe BigDecimal("11")
        }
    }
})
