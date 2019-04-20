package nl.pindab0ter.eggbot

import com.auxbrain.ei.EggInc
import com.github.kittinunf.fuel.core.Body
import com.jagrosh.jdautilities.command.CommandEvent
import org.joda.time.DateTime
import org.joda.time.Period
import org.joda.time.PeriodType
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder
import java.math.BigInteger
import java.math.BigInteger.ZERO
import java.text.DecimalFormat
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
fun <T> List<T>.init(): List<T> = take((size - 1).coerceAtLeast(0))

val monthAndDay: DateTimeFormatter = DateTimeFormatterBuilder()
    .appendMonthOfYearText()
    .appendLiteral(" ")
    .appendDayOfMonth(1)
    .toFormatter()
    .withLocale(Locale.UK)

val daysHoursAndMinutes: PeriodFormatter = PeriodFormatterBuilder()
    .printZeroNever()
    .appendDays()
    .appendSuffix("d")
    .appendHours()
    .appendSuffix("h")
    .appendMinutes()
    .appendSuffix("m")
    .toFormatter()
    .withLocale(Locale.UK)

fun BigInteger.formatForDisplay(): String = "%,d %%".format(this)
fun BigInteger.formatScientifically(): String = DecimalFormat("##0.###E0").format(toBigDecimal())

fun paddingSpaces(current: Any, longest: Any): String {
    val currentLength = current.toString().length
    val longestLength = longest.toString().length

    if (longestLength < currentLength) throw IllegalArgumentException("Longest's length ($longestLength) must be longer than current's length ($currentLength)")

    return " ".repeat(longestLength - currentLength)
}

fun <T : Any> paddingSpaces(current: T, containsLongest: Iterable<T>): String =
    paddingSpaces(current, containsLongest.maxBy { it.toString().length }!!)

fun <T : Any> StringBuilder.appendPaddingSpaces(current: T, longest: T): java.lang.StringBuilder =
    append(paddingSpaces(current, longest))

fun <T : Any> StringBuilder.appendPaddingSpaces(current: T, containsLongest: Iterable<T>): java.lang.StringBuilder =
    append(paddingSpaces(current, containsLongest))

fun String.splitMessage(
    prefix: String? = null,
    postFix: String? = null,
    separator: Char = '\n'
): List<String> = split(separator)
    .also { lines -> require(lines.none { it.length >= 2000 }) { "Any block cannot be larger than 2000 characters." } }
    .fold(listOf("")) { acc, section ->
        // If the section still fits in the last message, add it
        if (acc.last().length + section.length + (postFix?.length ?: 0) < 2000) {
            acc.init().plus(acc.last().plus(section))
        }
        // Otherwise start a new message
        else {
            acc.init().plus(acc.last().plus(postFix ?: "")).plus(prefix?.plus("\n") ?: "" + section)
        }
    }
    .let { it.init().plus(it.last().plus(postFix ?: "")) }


// Networking

fun Body.decodeBase64(): ByteArray = Base64.getDecoder().decode(toByteArray())


// Ease of access

val CommandEvent.arguments: List<String> get() = if (args.isBlank()) emptyList() else args.split(Regex("""\s+"""))

val EggInc.Game.soulBonus get() = epicResearchList.find { it.id == "soul_eggs" }!!.level
val EggInc.Game.prophecyBonus get() = epicResearchList.find { it.id == "prophecy_bonus" }!!.level


// Generic functions

inline fun <T> Iterable<T>.sumBy(selector: (T) -> BigInteger): BigInteger {
    var sum: BigInteger = ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}


// Exceptions

class PropertyNotFoundException(override val message: String?) : Exception(message)
