package nl.pindab0ter.eggbot

import com.auxbrain.ei.EggInc
import com.github.kittinunf.fuel.core.Body
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.entities.ChannelType
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.Period
import org.joda.time.PeriodType
import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.format.PeriodFormatterBuilder
import java.math.BigDecimal
import java.math.BigInteger
import java.math.BigInteger.ZERO
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.roundToLong

// Formatting

val EggInc.Egg.formattedName: String
    get() = name
        .toLowerCase()
        .split('_')
        .joinToString(" ", transform = String::capitalize)

fun Double.toDateTime(): DateTime = DateTime((this * 1000).roundToLong())
fun Double.toPeriod(): Period = Period((this * 1000).roundToLong()).normalizedStandard(PeriodType.dayTime())
fun Double.toDuration(): Duration = Duration((this * 1000).roundToLong())

fun Period.asDayHoursAndMinutes(): String = PeriodFormatterBuilder()
    .printZeroNever()
    .appendDays()
    .appendSuffix(" days")
    .appendSeparator(", ")
    .appendHours()
    .appendSuffix(" hours")
    .appendSeparator(" and ")
    .appendMinutes()
    .appendSuffix(" minutes")
    .toFormatter()
    .withLocale(Locale.UK)
    .print(this.normalizedStandard(PeriodType.dayTime()))

fun Duration.asDayHoursAndMinutes(): String = this.toPeriod().asDayHoursAndMinutes()

fun DateTime.asMonthAndDay(): String = DateTimeFormatterBuilder()
    .appendMonthOfYearText()
    .appendLiteral(" ")
    .appendDayOfMonth(1)
    .toFormatter()
    .withLocale(Locale.UK)
    .print(this)

fun DateTime.asDayHoursAndMinutes(): String = DateTimeFormatterBuilder()
    .appendDayOfWeekText()
    .appendLiteral(" ")
    .appendDayOfMonth(1)
    .appendLiteral(" ")
    .appendMonthOfYearText()
    .appendLiteral(" ")
    .appendHourOfDay(1)
    .appendLiteral(":")
    .appendMinuteOfHour(2)
    .toFormatter()
    .withLocale(Locale.UK)
    .print(this)

// val displayFormat = DecimalFormat(",###.###", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
val displayFormat = DecimalFormat(",###.###", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
val roundedDisplayFormat = DecimalFormat(",###", DecimalFormatSymbols.getInstance(Locale.ENGLISH))

fun Double.formatForDisplay(): String = displayFormat.format(this)
fun Long.formatForDisplay(): String = displayFormat.format(this)
fun Int.formatForDisplay(): String = displayFormat.format(this)
fun BigInteger.formatForDisplay(): String = displayFormat.format(this)

fun Double.formatIllionsLong(rounded: Boolean = false): String {
    val (formatter, value) = if (rounded) (roundedDisplayFormat to this.round()) else (displayFormat to this)
    return when (value) {
        in (1.0e006..1.0e009 - 1) -> "${formatter.format(value / 1e006)} Million"
        in (1.0e009..1.0e012 - 1) -> "${formatter.format(value / 1e009)} Billion"
        in (1.0e012..1.0e015 - 1) -> "${formatter.format(value / 1e012)} Trillion"
        in (1.0e015..1.0e018 - 1) -> "${formatter.format(value / 1e015)} Quadrillion"
        in (1.0e018..1.0e021 - 1) -> "${formatter.format(value / 1e018)} Quintillion"
        in (1.0e021..1.0e024 - 1) -> "${formatter.format(value / 1e021)} Sextillion"
        in (1.0e024..1.0e027 - 1) -> "${formatter.format(value / 1e024)} Septillion"
        in (1.0e027..1.0e030 - 1) -> "${formatter.format(value / 1e027)} Octillion"
        in (1.0e030..1.0e033 - 1) -> "${formatter.format(value / 1e030)} Nonillion"
        in (1.0e033..1.0e036 - 1) -> "${formatter.format(value / 1e033)} Decillion"
        in (1.0e036..1.0e039 - 1) -> "${formatter.format(value / 1e036)} Undecillion"
        in (1.0e039..1.0e042 - 1) -> "${formatter.format(value / 1e039)} Duodecillion"
        in (1.0e042..1.0e045 - 1) -> "${formatter.format(value / 1e042)} Tredecillion"
        in (1.0e045..1.0e048 - 1) -> "${formatter.format(value / 1e045)} Quattuordecillion"
        in (1.0e048..1.0e051 - 1) -> "${formatter.format(value / 1e048)} Quinquadecillion"
        in (1.0e051..1.0e054 - 1) -> "${formatter.format(value / 1e051)} Sedecillion"
        in (1.0e054..1.0e057 - 1) -> "${formatter.format(value / 1e054)} Septendecillion"
        in (1.0e057..1.0e060 - 1) -> "${formatter.format(value / 1e057)} Octodecillion"
        in (1.0e060..1.0e063 - 1) -> "${formatter.format(value / 1e060)} Novendecillion"
        in (1.0e063..1.0e066 - 1) -> "${formatter.format(value / 1e063)} Vigintillion"
        in (1.0e066..1.0e069 - 1) -> "${formatter.format(value / 1e066)} Unvigintillion"
        in (1.0e069..1.0e072 - 1) -> "${formatter.format(value / 1e069)} Duovigintillion"
        in (1.0e072..1.0e075 - 1) -> "${formatter.format(value / 1e072)} Tresvigintillion"
        in (1.0e075..1.0e078 - 1) -> "${formatter.format(value / 1e075)} Quattuorvigintillion"
        in (1.0e078..1.0e081 - 1) -> "${formatter.format(value / 1e078)} Quinquavigintillion"
        in (1.0e081..1.0e084 - 1) -> "${formatter.format(value / 1e081)} Sesvigintillion"
        in (1.0e084..1.0e087 - 1) -> "${formatter.format(value / 1e084)} Septemvigintillion"
        in (1.0e087..1.0e090 - 1) -> "${formatter.format(value / 1e087)} Octovigintillion"
        in (1.0e090..1.0e093 - 1) -> "${formatter.format(value / 1e090)} Novemvigintillion"
        in (1.0e093..1.0e096 - 1) -> "${formatter.format(value / 1e093)} Trigintillion"
        in (1.0e096..1.0e099 - 1) -> "${formatter.format(value / 1e096)} Untrigintillion"
        in (1.0e099..1.0e102 - 1) -> "${formatter.format(value / 1e099)} Duotrigintillion"
        in (1.0e102..1.0e105 - 1) -> "${formatter.format(value / 1e102)} Trestrigintillion"
        in (1.0e105..1.0e108 - 1) -> "${formatter.format(value / 1e105)} Quattuortrigintillion"
        in (1.0e108..1.0e111 - 1) -> "${formatter.format(value / 1e108)} Quinquatrigintillion"
        in (1.0e111..1.0e114 - 1) -> "${formatter.format(value / 1e111)} Sestrigintillion"
        in (1.0e114..1.0e117 - 1) -> "${formatter.format(value / 1e114)} Septentrigintillion"
        in (1.0e117..1.0e120 - 1) -> "${formatter.format(value / 1e117)} Octotrigintillion"
        in (1.0e120..1.0e123 - 1) -> "${formatter.format(value / 1e120)} Noventrigintillion"
        else -> BigDecimal(value).toBigInteger().formatForDisplay()
    }
}

fun Double.formatIllions(rounded: Boolean = false): String {
    val (formatter, value) = if (rounded) (roundedDisplayFormat to this.round()) else (displayFormat to this)
    return when (value) {
        in (1.0e006..1.0e009 - 1) -> "${formatter.format(value / 1e006)}M"
        in (1.0e009..1.0e012 - 1) -> "${formatter.format(value / 1e009)}B"
        in (1.0e012..1.0e015 - 1) -> "${formatter.format(value / 1e012)}T"
        in (1.0e015..1.0e018 - 1) -> "${formatter.format(value / 1e015)}q"
        in (1.0e018..1.0e021 - 1) -> "${formatter.format(value / 1e018)}Q"
        in (1.0e021..1.0e024 - 1) -> "${formatter.format(value / 1e021)}s"
        in (1.0e024..1.0e027 - 1) -> "${formatter.format(value / 1e024)}S"
        in (1.0e027..1.0e030 - 1) -> "${formatter.format(value / 1e027)}O"
        in (1.0e030..1.0e033 - 1) -> "${formatter.format(value / 1e030)}N"
        in (1.0e033..1.0e036 - 1) -> "${formatter.format(value / 1e033)}D"
        in (1.0e036..1.0e039 - 1) -> "${formatter.format(value / 1e036)}uD"
        in (1.0e039..1.0e042 - 1) -> "${formatter.format(value / 1e039)}dD"
        in (1.0e042..1.0e045 - 1) -> "${formatter.format(value / 1e042)}tD"
        in (1.0e045..1.0e048 - 1) -> "${formatter.format(value / 1e045)}qD"
        in (1.0e048..1.0e051 - 1) -> "${formatter.format(value / 1e048)}QD"
        in (1.0e051..1.0e054 - 1) -> "${formatter.format(value / 1e051)}sD"
        in (1.0e054..1.0e057 - 1) -> "${formatter.format(value / 1e054)}SD"
        in (1.0e057..1.0e060 - 1) -> "${formatter.format(value / 1e057)}OD"
        in (1.0e060..1.0e063 - 1) -> "${formatter.format(value / 1e060)}ND"
        in (1.0e063..1.0e066 - 1) -> "${formatter.format(value / 1e063)}V"
        in (1.0e066..1.0e069 - 1) -> "${formatter.format(value / 1e066)}uV"
        in (1.0e069..1.0e072 - 1) -> "${formatter.format(value / 1e069)}dV"
        in (1.0e072..1.0e075 - 1) -> "${formatter.format(value / 1e072)}tV"
        in (1.0e075..1.0e078 - 1) -> "${formatter.format(value / 1e075)}qV"
        in (1.0e078..1.0e081 - 1) -> "${formatter.format(value / 1e078)}QV"
        in (1.0e081..1.0e084 - 1) -> "${formatter.format(value / 1e081)}sV"
        in (1.0e084..1.0e087 - 1) -> "${formatter.format(value / 1e084)}SV"
        in (1.0e087..1.0e090 - 1) -> "${formatter.format(value / 1e087)}OV"
        in (1.0e090..1.0e093 - 1) -> "${formatter.format(value / 1e090)}NV"
        in (1.0e093..1.0e096 - 1) -> "${formatter.format(value / 1e093)}Tg"
        in (1.0e096..1.0e099 - 1) -> "${formatter.format(value / 1e096)}uTG"
        in (1.0e099..1.0e102 - 1) -> "${formatter.format(value / 1e099)}dTg"
        in (1.0e102..1.0e105 - 1) -> "${formatter.format(value / 1e102)}tTg"
        in (1.0e105..1.0e108 - 1) -> "${formatter.format(value / 1e105)}qTg"
        in (1.0e108..1.0e111 - 1) -> "${formatter.format(value / 1e108)}QTg"
        in (1.0e111..1.0e114 - 1) -> "${formatter.format(value / 1e111)}sTg"
        in (1.0e114..1.0e117 - 1) -> "${formatter.format(value / 1e114)}STg"
        in (1.0e117..1.0e120 - 1) -> "${formatter.format(value / 1e117)}OTg"
        in (1.0e120..1.0e123 - 1) -> "${formatter.format(value / 1e120)}NTg"
        else -> BigDecimal(value).toBigInteger().formatForDisplay()
    }
}

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

fun <T : Any> StringBuilder.appendPaddingSpaces(
    current: T,
    containsLongest: Iterable<T>
): java.lang.StringBuilder =
    append(paddingSpaces(current, containsLongest))

fun String.splitMessage(
    prefix: String = "",
    postfix: String = "",
    separator: Char = '\n'
): List<String> = split(separator)
    .also { lines -> require(lines.none { it.length >= 2000 }) { "Any block cannot be larger than 2000 characters." } }
    .fold(listOf("")) { acc, section ->
        if ("${acc.last()}$section$postfix$separator".length < 2000) acc.replaceLast { "$it$section$separator" }
        else acc.replaceLast { "$it$postfix" }.plus("$prefix$section$separator")
    }
    .replaceLast { "$it$postfix" }


// Networking

fun Body.decodeBase64(): ByteArray = Base64.getDecoder().decode(toByteArray())


// Ease of access

val CommandEvent.arguments: List<String> get() = if (args.isBlank()) emptyList() else args.split(Regex("""\s+"""))

val EggInc.Game.soulBonus get() = epicResearchList.find { it.id == "soul_eggs" }!!.level
val EggInc.Game.prophecyBonus get() = epicResearchList.find { it.id == "prophecy_bonus" }!!.level

fun <T> Iterable<T>.init() = take((count() - 1).coerceAtLeast(0))
fun <T> Iterable<T>.tail() = drop(1)
fun <T> Iterable<T>.replaceLast(block: (T) -> T) = init().plus(block(last()))


// Maths

inline fun <T> Iterable<T>.sumBy(selector: (T) -> BigInteger): BigInteger {
    var sum: BigInteger = ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

fun Double.round(places: Int = 0) = BigDecimal(this).setScale(places, RoundingMode.HALF_UP).toDouble()

// Exceptions

class PropertyNotFoundException(override val message: String?) : Exception(message)


// JDA Utilities

fun CommandEvent.replyInDms(messages: List<String>) {
    var successful: Boolean? = null
    messages.forEachIndexed { i, message ->
        replyInDm(message, {
            successful = (successful ?: true) && true
            if (i == messages.size - 1 && isFromType(ChannelType.TEXT)) reactSuccess()
        }, {
            if (successful == null) replyWarning("Help cannot be sent because you are blocking Direct Messages.")
            successful = false
        })
    }
}


// Messages

val Command.missingArguments get() = "Missing argument(s). Use `${EggBot.commandClient.textualPrefix}${this.name} ${this.arguments}` without the brackets."

val Command.tooManyArguments get() = "Too many arguments. Use `${EggBot.commandClient.textualPrefix}${this.name} ${this.arguments}` without the brackets."
