package nl.pindab0ter.eggbot.utilities

import org.joda.time.Duration
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP

inline fun <T> Iterable<T>.sumByBigDecimal(selector: (T) -> BigDecimal): BigDecimal {
    var sum: BigDecimal = BigDecimal.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

operator fun Duration.div(other: Duration): Double? = try {
    this.millis.toDouble() / other.millis.toDouble()
} catch (e: Exception) {
    null
}

fun Iterable<BigDecimal>.product(): BigDecimal = reduce { acc, bonus -> acc * bonus }
fun List<BigDecimal>.sum(): BigDecimal = this.reduce { acc, duration -> acc + duration }
fun List<Duration>.sum(): Duration = this.reduce { acc, duration -> acc + duration }
operator fun Int.times(other: BigDecimal): BigDecimal = this.toBigDecimal() * other
operator fun BigDecimal.times(other: Int): BigDecimal = this.multiply(other.toBigDecimal())
operator fun BigDecimal.times(other: Long): BigDecimal = this.multiply(other.toBigDecimal())
operator fun BigDecimal.times(other: Duration): BigDecimal = this.multiply(other.standardSeconds.toBigDecimal())
operator fun BigDecimal.div(other: BigDecimal): BigDecimal = this.divide(other, 6, HALF_UP)
fun BigDecimal.round(scale: Int = 0): BigDecimal = this.setScale(scale, HALF_UP)
val Int.odd: Boolean get() = this % 2 == 1
val Int.even: Boolean get() = this % 2 == 0