package nl.pindab0ter.eggbot.helpers

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import java.math.BigDecimal

/**
 * Creates a matcher for the interval [[this] - [tolerance] , [this] + [tolerance]]
 *
 *
 * ```
 * BigDecimal("0.1") shouldBe (BigDecimal("0.4") plusOrMinus BigDecimal("0.5"))   // Assertion passes
 * BigDecimal("0.1") shouldBe (BigDecimal("0.4") plusOrMinus BigDecimal("0.2"))   // Assertion fails
 * ```
 */
infix fun BigDecimal.plusOrMinus(tolerance: BigDecimal): ToleranceMatcher {
    require(tolerance >= BigDecimal.ZERO)
    return ToleranceMatcher(this, tolerance)
}

class ToleranceMatcher(private val expected: BigDecimal?, private val tolerance: BigDecimal) : Matcher<BigDecimal?> {

    override fun test(value: BigDecimal?): MatcherResult {
        return if (value == null || expected == null) {
            MatcherResult(value == expected,
                "$value should be equal to $expected",
                "$value should not be equal to $expected")
        } else {
            if (tolerance == BigDecimal.ZERO)
                println("[WARN] When comparing doubles consider using tolerance, eg: a shouldBe (b plusOrMinus c)")
            val diff = (value - expected).abs()

            val passed = diff <= tolerance
            val low = expected - tolerance
            val high = expected + tolerance
            val message = when (tolerance) {
                BigDecimal.ZERO -> "$value should be equal to $expected"
                else -> "$value should be equal to $expected within tolerance of $tolerance (lowest acceptable value is $low; highest acceptable value is $high)"
            }
            MatcherResult(passed, message, "$value should not be equal to $expected")
        }
    }
}
