package nl.pindab0ter.eggbot.helpers

import org.joda.time.Duration
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode.*

val mathContext = MathContext(6, HALF_UP)

class BigDecimal {
    companion object {
        val FOUR = BigDecimal(4L)
        val FIVE = BigDecimal(5L)
        val SIXTY = BigDecimal(60L)
        val HUNDRED = BigDecimal(100L)
        val ONE_HUNDRED_FORTY = BigDecimal(140L)
        val FIFTY = BigDecimal(50L)
        val THOUSAND = BigDecimal(1000L)
        val FIVE_HUNDRED_QUINTILLION = BigDecimal("500000000000000000000")
    }
}

inline fun <T> Iterable<T>.sumByBigDecimal(selector: (T) -> BigDecimal): BigDecimal {
    var sum: BigDecimal = BigDecimal.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

fun BigDecimal.nextPowerOfTen(): BigDecimal = BigDecimal("1".padEnd(setScale(0, FLOOR).toPlainString().length + 1, '0'))
fun BigDecimal.floor(): BigDecimal = setScale(0, FLOOR)
fun BigDecimal.ceiling(): BigDecimal = setScale(0, CEILING)

fun Iterable<BigDecimal>.product(): BigDecimal = reduce { acc, bonus -> acc * bonus }
fun List<BigDecimal>.sum(): BigDecimal = this.reduce { acc, duration -> acc + duration }
operator fun Int.times(other: BigDecimal): BigDecimal = this.toBigDecimal() * other
operator fun BigDecimal.times(other: Int): BigDecimal = this.multiply(other.toBigDecimal())
operator fun BigDecimal.times(other: Long): BigDecimal = this.multiply(other.toBigDecimal())
operator fun BigDecimal.times(other: Duration): BigDecimal = this.multiply(other.standardSeconds.toBigDecimal())
operator fun BigDecimal.div(other: BigDecimal): BigDecimal = this.divide(other, mathContext)
fun BigDecimal.round(scale: Int = 0): BigDecimal = this.setScale(scale, HALF_UP)