package nl.pindab0ter.eggbot.model

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import nl.pindab0ter.eggbot.helpers.BigDecimal.Companion.FIFTY
import nl.pindab0ter.eggbot.helpers.BigDecimal.Companion.FIVE
import nl.pindab0ter.eggbot.helpers.BigDecimal.Companion.FIVE_HUNDRED_QUINTILLION
import nl.pindab0ter.eggbot.helpers.BigDecimal.Companion.HUNDRED
import nl.pindab0ter.eggbot.helpers.BigDecimal.Companion.ONE_HUNDRED_FOURTY
import nl.pindab0ter.eggbot.helpers.BigDecimal.Companion.THOUSAND
import java.math.BigDecimal
import java.math.BigDecimal.*

class EarningsBonusTest : DescribeSpec({
    describe("soulEggsToNextRank") {
        // TODO: Change scenarios to more reflect actual usage scenarios; From a rank to a rank
        // TODO: Use partial application to pre-fill functions
        context("No research") {
            soulEggsToNextRank(THOUSAND, ZERO, ZERO, ZERO) shouldBe BigDecimal(48L)
            soulEggsToNextRank(THOUSAND, ONE, ZERO, ZERO) shouldBe BigDecimal(48L)
            soulEggsToNextRank(THOUSAND, TEN, ZERO, ZERO) shouldBe BigDecimal(48L)
            soulEggsToNextRank(THOUSAND, HUNDRED, ZERO, ZERO) shouldBe BigDecimal(142L)

            soulEggsToNextRank(FIVE_HUNDRED_QUINTILLION, ZERO, ZERO, ZERO) shouldBe BigDecimal(15L)
            soulEggsToNextRank(FIVE_HUNDRED_QUINTILLION, ONE, ZERO, ZERO) shouldBe BigDecimal(15L)
            soulEggsToNextRank(FIVE_HUNDRED_QUINTILLION, TEN, ZERO, ZERO) shouldBe BigDecimal(15L)
            soulEggsToNextRank(FIVE_HUNDRED_QUINTILLION, HUNDRED, ZERO, ZERO) shouldBe BigDecimal(109L)
        }

        context("Fifty levels of Soul Egg research") {
            soulEggsToNextRank(THOUSAND, ZERO, FIFTY, ZERO) shouldBe BigDecimal(11L)
            soulEggsToNextRank(THOUSAND, ONE, FIFTY, ZERO) shouldBe BigDecimal(11L)
            soulEggsToNextRank(THOUSAND, TEN, FIFTY, ZERO) shouldBe BigDecimal(11L)
            soulEggsToNextRank(THOUSAND, HUNDRED, FIFTY, ZERO) shouldBe BigDecimal(105L)

            soulEggsToNextRank(FIVE_HUNDRED_QUINTILLION, ZERO, FIFTY, ZERO) shouldBe BigDecimal(25L)
            soulEggsToNextRank(FIVE_HUNDRED_QUINTILLION, ONE, FIFTY, ZERO) shouldBe BigDecimal(25L)
            soulEggsToNextRank(FIVE_HUNDRED_QUINTILLION, TEN, FIFTY, ZERO) shouldBe BigDecimal(25L)
            soulEggsToNextRank(FIVE_HUNDRED_QUINTILLION, HUNDRED, FIFTY, ZERO) shouldBe BigDecimal(120L)
        }

        context("One level of Prophecy Egg research") {
            soulEggsToNextRank(THOUSAND, ZERO, ZERO, ONE) shouldBe BigDecimal(11L)
            soulEggsToNextRank(THOUSAND, ONE, ZERO, ONE) shouldBe BigDecimal(11L)
            soulEggsToNextRank(THOUSAND, TEN, ZERO, ONE) shouldBe BigDecimal(11L)
            soulEggsToNextRank(THOUSAND, HUNDRED, ZERO, ONE) shouldBe BigDecimal(105L)

            soulEggsToNextRank(FIVE_HUNDRED_QUINTILLION, ZERO, ZERO, ONE) shouldBe BigDecimal(40L)
            soulEggsToNextRank(FIVE_HUNDRED_QUINTILLION, ONE, ZERO, ONE) shouldBe BigDecimal(40L)
            soulEggsToNextRank(FIVE_HUNDRED_QUINTILLION, TEN, ZERO, ONE) shouldBe BigDecimal(40L)
            soulEggsToNextRank(FIVE_HUNDRED_QUINTILLION, HUNDRED, ZERO, ONE) shouldBe BigDecimal(119L)
        }

        context("Maxed out research") {
            soulEggsToNextRank(THOUSAND, ZERO, ONE_HUNDRED_FOURTY, FIVE) shouldBe BigDecimal(20L)
            soulEggsToNextRank(THOUSAND, ONE, ONE_HUNDRED_FOURTY, FIVE) shouldBe BigDecimal(20L)
            soulEggsToNextRank(THOUSAND, TEN, ONE_HUNDRED_FOURTY, FIVE) shouldBe BigDecimal(20L)
            soulEggsToNextRank(THOUSAND, HUNDRED, ONE_HUNDRED_FOURTY, FIVE) shouldBe BigDecimal(117L)

            soulEggsToNextRank(FIVE_HUNDRED_QUINTILLION, ZERO, ONE_HUNDRED_FOURTY, FIVE) shouldBe BigDecimal(4L)
            soulEggsToNextRank(FIVE_HUNDRED_QUINTILLION, ONE, ONE_HUNDRED_FOURTY, FIVE) shouldBe BigDecimal(4L)
            soulEggsToNextRank(FIVE_HUNDRED_QUINTILLION, TEN, ONE_HUNDRED_FOURTY, FIVE) shouldBe BigDecimal(28L)
            soulEggsToNextRank(FIVE_HUNDRED_QUINTILLION, HUNDRED, ONE_HUNDRED_FOURTY, FIVE) shouldBe BigDecimal(124L)
        }
    }
})
