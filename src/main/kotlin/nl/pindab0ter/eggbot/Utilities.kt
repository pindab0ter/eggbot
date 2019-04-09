package nl.pindab0ter.eggbot

import com.auxbrain.ei.EggInc
import com.github.kittinunf.fuel.core.Body
import com.jagrosh.jdautilities.command.CommandEvent
import org.joda.time.DateTime
import org.joda.time.Period
import org.joda.time.PeriodType
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder
import java.math.BigInteger
import java.math.BigInteger.ZERO
import java.util.*
import kotlin.math.roundToLong

// Formatting

val EggInc.Egg.formattedName: String
    get() = name
        .toLowerCase()
        .split('_')
        .joinToString(" ", transform = String::capitalize)

fun Double.toDateTime(): DateTime = DateTime((this * 1000).roundToLong())
fun Double.toPeriod(): Period = Period((this * 1000).roundToLong()).normalizedStandard(PeriodType.days())

val daysHoursAndMinutes: PeriodFormatter = PeriodFormatterBuilder()
    .printZeroNever()
    .appendDays()
    .appendSuffix("d")
    .appendHours()
    .appendSuffix("h")
    .appendMinutes()
    .appendSuffix("m")
    .toFormatter()

fun format(earningsBonus: BigInteger): String = "%,d %%".format(earningsBonus)


// Networking

fun Body.decodeBase64(): ByteArray = Base64.getDecoder().decode(toByteArray())


// Ease of access

val CommandEvent.arguments: List<String>
    get() = if (args.isBlank()) emptyList() else args.split(Regex("""\s+"""))


// Generic functions

inline fun <T> Iterable<T>.sumBy(selector: (T) -> BigInteger): BigInteger {
    var sum: BigInteger = ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

fun Iterable<BigInteger>.sum(): BigInteger {
    var sum: BigInteger = ZERO
    for (element in this) {
        sum += element
    }
    return sum
}
