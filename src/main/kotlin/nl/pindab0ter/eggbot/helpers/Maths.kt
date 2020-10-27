package nl.pindab0ter.eggbot.helpers

import org.joda.time.Duration
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.math.RoundingMode.HALF_UP

val mathContext = MathContext(6, HALF_UP)

class BigDecimal {
    companion object {
        val FOUR = BigDecimal(4L)
        val SIXTY = BigDecimal(60L)
    }
}

inline fun <T> Iterable<T>.sumByBigDecimal(selector: (T) -> BigDecimal): BigDecimal {
    var sum: BigDecimal = BigDecimal.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

fun Iterable<BigDecimal>.product(): BigDecimal = reduce { acc, bonus -> acc * bonus }
fun List<BigDecimal>.sum(): BigDecimal = this.reduce { acc, duration -> acc + duration }
operator fun Int.times(other: BigDecimal): BigDecimal = this.toBigDecimal() * other
operator fun BigDecimal.times(other: Int): BigDecimal = this.multiply(other.toBigDecimal())
operator fun BigDecimal.times(other: Long): BigDecimal = this.multiply(other.toBigDecimal())
operator fun BigDecimal.times(other: Duration): BigDecimal = this.multiply(other.standardSeconds.toBigDecimal())
operator fun BigDecimal.div(other: BigDecimal): BigDecimal = this.divide(other, mathContext)
infix fun BigDecimal.equals(other: BigDecimal) = this.compareTo(other) == 0
fun BigDecimal.floor(): BigDecimal = setScale(0, RoundingMode.FLOOR)
fun BigDecimal.ceiling(): BigDecimal = setScale(0, RoundingMode.CEILING)