package nl.pindab0ter.eggbot.helpers

import org.joda.time.Duration
import java.math.BigDecimal
import java.math.BigDecimal.ONE
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

fun Iterable<BigDecimal>.product(): BigDecimal = reduce(BigDecimal::times)
inline fun <T> Iterable<T>.productOf(selector: (T) -> BigDecimal): BigDecimal = fold(ONE) { acc, element ->
    acc * selector(element)
}

fun Iterable<BigDecimal>.sum(): BigDecimal = reduce(BigDecimal::plus)
operator fun Int.times(other: BigDecimal): BigDecimal = toBigDecimal() * other
operator fun BigDecimal.times(other: Int): BigDecimal = multiply(other.toBigDecimal())
operator fun BigDecimal.times(other: Long): BigDecimal = multiply(other.toBigDecimal())
operator fun BigDecimal.times(other: Duration): BigDecimal = multiply(other.standardSeconds.toBigDecimal())
operator fun BigDecimal.div(other: BigDecimal): BigDecimal = divide(other, mathContext)
infix fun BigDecimal.equals(other: BigDecimal) = compareTo(other) == 0
fun BigDecimal.floor(): BigDecimal = setScale(0, RoundingMode.FLOOR)
fun BigDecimal.ceiling(): BigDecimal = setScale(0, RoundingMode.CEILING)