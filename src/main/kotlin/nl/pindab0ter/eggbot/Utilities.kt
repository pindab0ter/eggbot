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
import java.math.BigDecimal.*
import java.math.MathContext
import java.math.MathContext.*
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

val decimalFormat = DecimalFormat(",###.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
val integerFormat = DecimalFormat(",###", DecimalFormatSymbols.getInstance(Locale.ENGLISH))

fun Double.formatDecimal(): String = decimalFormat.format(this)
fun Long.formatDecimal(): String = decimalFormat.format(this)
fun Int.formatDecimal(): String = decimalFormat.format(this)
fun BigDecimal.formatDecimal(): String = decimalFormat.format(this.round(UNLIMITED))
fun BigDecimal.formatInteger(): String = integerFormat.format(this)

fun BigDecimal.formatIllionsLong(rounded: Boolean = false): String {
    val formatter = if (rounded) integerFormat else decimalFormat
    return when (this) {
        in (TEN.pow(6)..TEN.pow(9).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(6)))} Million"
        in (TEN.pow(9)..TEN.pow(12).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(9)))} Billion"
        in (TEN.pow(12)..TEN.pow(15).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(12)))} Trillion"
        in (TEN.pow(15)..TEN.pow(18).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(15)))} Quadrillion"
        in (TEN.pow(18)..TEN.pow(21).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(18)))} Quintillion"
        in (TEN.pow(21)..TEN.pow(24).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(21)))} Sextillion"
        in (TEN.pow(24)..TEN.pow(27).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(24)))} Septillion"
        in (TEN.pow(27)..TEN.pow(30).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(27)))} Octillion"
        in (TEN.pow(30)..TEN.pow(33).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(30)))} Nonillion"
        in (TEN.pow(33)..TEN.pow(36).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(33)))} Decillion"
        in (TEN.pow(36)..TEN.pow(39).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(36)))} Undecillion"
        in (TEN.pow(39)..TEN.pow(42).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(39)))} Duodecillion"
        in (TEN.pow(42)..TEN.pow(45).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(42)))} Tredecillion"
        in (TEN.pow(45)..TEN.pow(48).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(45)))} Quattuordecillion"
        in (TEN.pow(48)..TEN.pow(51).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(48)))} Quinquadecillion"
        in (TEN.pow(51)..TEN.pow(54).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(51)))} Sedecillion"
        in (TEN.pow(54)..TEN.pow(57).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(54)))} Septendecillion"
        in (TEN.pow(57)..TEN.pow(60).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(57)))} Octodecillion"
        in (TEN.pow(60)..TEN.pow(63).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(60)))} Novendecillion"
        in (TEN.pow(63)..TEN.pow(66).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(63)))} Vigintillion"
        in (TEN.pow(66)..TEN.pow(69).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(66)))} Unvigintillion"
        in (TEN.pow(69)..TEN.pow(72).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(69)))} Duovigintillion"
        in (TEN.pow(72)..TEN.pow(75).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(72)))} Tresvigintillion"
        in (TEN.pow(75)..TEN.pow(78).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(75)))} Quattuorvigintillion"
        in (TEN.pow(78)..TEN.pow(81).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(78)))} Quinquavigintillion"
        in (TEN.pow(81)..TEN.pow(84).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(81)))} Sesvigintillion"
        in (TEN.pow(84)..TEN.pow(87).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(84)))} Septemvigintillion"
        in (TEN.pow(87)..TEN.pow(90).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(87)))} Octovigintillion"
        in (TEN.pow(90)..TEN.pow(93).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(90)))} Novemvigintillion"
        in (TEN.pow(93)..TEN.pow(96).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(93)))} Trigintillion"
        in (TEN.pow(96)..TEN.pow(99).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(96)))} Untrigintillion"
        in (TEN.pow(99)..TEN.pow(102).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(99)))} Duotrigintillion"
        in (TEN.pow(102)..TEN.pow(105).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(102)))} Trestrigintillion"
        in (TEN.pow(105)..TEN.pow(108).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(105)))} Quattuortrigintillion"
        in (TEN.pow(108)..TEN.pow(111).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(108)))} Quinquatrigintillion"
        in (TEN.pow(111)..TEN.pow(114).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(111)))} Sestrigintillion"
        in (TEN.pow(114)..TEN.pow(117).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(114)))} Septentrigintillion"
        in (TEN.pow(117)..TEN.pow(120).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(117)))} Octotrigintillion"
        in (TEN.pow(120)..TEN.pow(123).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(120)))} Noventrigintillion"
        else -> formatter.format(this)
    }
}

fun Double.formatIllions(rounded: Boolean = false): String {
    val formatter = if (rounded) integerFormat else decimalFormat
    return when (this) {
        in (1.0e006..1.0e009 - 1) -> "${formatter.format(this / 1e006)}M"
        in (1.0e009..1.0e012 - 1) -> "${formatter.format(this / 1e009)}B"
        in (1.0e012..1.0e015 - 1) -> "${formatter.format(this / 1e012)}T"
        in (1.0e015..1.0e018 - 1) -> "${formatter.format(this / 1e015)}q"
        in (1.0e018..1.0e021 - 1) -> "${formatter.format(this / 1e018)}Q"
        in (1.0e021..1.0e024 - 1) -> "${formatter.format(this / 1e021)}s"
        in (1.0e024..1.0e027 - 1) -> "${formatter.format(this / 1e024)}S"
        in (1.0e027..1.0e030 - 1) -> "${formatter.format(this / 1e027)}O"
        in (1.0e030..1.0e033 - 1) -> "${formatter.format(this / 1e030)}N"
        in (1.0e033..1.0e036 - 1) -> "${formatter.format(this / 1e033)}D"
        in (1.0e036..1.0e039 - 1) -> "${formatter.format(this / 1e036)}uD"
        in (1.0e039..1.0e042 - 1) -> "${formatter.format(this / 1e039)}dD"
        in (1.0e042..1.0e045 - 1) -> "${formatter.format(this / 1e042)}tD"
        in (1.0e045..1.0e048 - 1) -> "${formatter.format(this / 1e045)}qD"
        in (1.0e048..1.0e051 - 1) -> "${formatter.format(this / 1e048)}QD"
        in (1.0e051..1.0e054 - 1) -> "${formatter.format(this / 1e051)}sD"
        in (1.0e054..1.0e057 - 1) -> "${formatter.format(this / 1e054)}SD"
        in (1.0e057..1.0e060 - 1) -> "${formatter.format(this / 1e057)}OD"
        in (1.0e060..1.0e063 - 1) -> "${formatter.format(this / 1e060)}ND"
        in (1.0e063..1.0e066 - 1) -> "${formatter.format(this / 1e063)}V"
        in (1.0e066..1.0e069 - 1) -> "${formatter.format(this / 1e066)}uV"
        in (1.0e069..1.0e072 - 1) -> "${formatter.format(this / 1e069)}dV"
        in (1.0e072..1.0e075 - 1) -> "${formatter.format(this / 1e072)}tV"
        in (1.0e075..1.0e078 - 1) -> "${formatter.format(this / 1e075)}qV"
        in (1.0e078..1.0e081 - 1) -> "${formatter.format(this / 1e078)}QV"
        in (1.0e081..1.0e084 - 1) -> "${formatter.format(this / 1e081)}sV"
        in (1.0e084..1.0e087 - 1) -> "${formatter.format(this / 1e084)}SV"
        in (1.0e087..1.0e090 - 1) -> "${formatter.format(this / 1e087)}OV"
        in (1.0e090..1.0e093 - 1) -> "${formatter.format(this / 1e090)}NV"
        in (1.0e093..1.0e096 - 1) -> "${formatter.format(this / 1e093)}Tg"
        in (1.0e096..1.0e099 - 1) -> "${formatter.format(this / 1e096)}uTG"
        in (1.0e099..1.0e102 - 1) -> "${formatter.format(this / 1e099)}dTg"
        in (1.0e102..1.0e105 - 1) -> "${formatter.format(this / 1e102)}tTg"
        in (1.0e105..1.0e108 - 1) -> "${formatter.format(this / 1e105)}qTg"
        in (1.0e108..1.0e111 - 1) -> "${formatter.format(this / 1e108)}QTg"
        in (1.0e111..1.0e114 - 1) -> "${formatter.format(this / 1e111)}sTg"
        in (1.0e114..1.0e117 - 1) -> "${formatter.format(this / 1e114)}STg"
        in (1.0e117..1.0e120 - 1) -> "${formatter.format(this / 1e117)}OTg"
        in (1.0e120..1.0e123 - 1) -> "${formatter.format(this / 1e120)}NTg"
        else -> formatter.format(this)
    }
}

fun BigDecimal.formatIllions(rounded: Boolean = false): String {
    val formatter = if (rounded) integerFormat else decimalFormat
    return when (this) {
        in (TEN.pow(6)..TEN.pow(9).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(6)))}M"
        in (TEN.pow(9)..TEN.pow(12).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(9)))}B"
        in (TEN.pow(12)..TEN.pow(15).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(12)))}T"
        in (TEN.pow(15)..TEN.pow(18).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(15)))}q"
        in (TEN.pow(18)..TEN.pow(21).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(18)))}Q"
        in (TEN.pow(21)..TEN.pow(24).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(21)))}s"
        in (TEN.pow(24)..TEN.pow(27).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(24)))}S"
        in (TEN.pow(27)..TEN.pow(30).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(27)))}O"
        in (TEN.pow(30)..TEN.pow(33).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(30)))}N"
        in (TEN.pow(33)..TEN.pow(36).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(33)))}D"
        in (TEN.pow(36)..TEN.pow(39).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(36)))}uD"
        in (TEN.pow(39)..TEN.pow(42).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(39)))}dD"
        in (TEN.pow(42)..TEN.pow(45).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(42)))}tD"
        in (TEN.pow(45)..TEN.pow(48).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(45)))}qD"
        in (TEN.pow(48)..TEN.pow(51).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(48)))}QD"
        in (TEN.pow(51)..TEN.pow(54).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(51)))}sD"
        in (TEN.pow(54)..TEN.pow(57).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(54)))}SD"
        in (TEN.pow(57)..TEN.pow(60).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(57)))}OD"
        in (TEN.pow(60)..TEN.pow(63).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(60)))}ND"
        in (TEN.pow(63)..TEN.pow(66).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(63)))}V"
        in (TEN.pow(66)..TEN.pow(69).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(66)))}uV"
        in (TEN.pow(69)..TEN.pow(72).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(69)))}dV"
        in (TEN.pow(72)..TEN.pow(75).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(72)))}tV"
        in (TEN.pow(75)..TEN.pow(78).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(75)))}qV"
        in (TEN.pow(78)..TEN.pow(81).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(78)))}QV"
        in (TEN.pow(81)..TEN.pow(84).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(81)))}sV"
        in (TEN.pow(84)..TEN.pow(87).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(84)))}SV"
        in (TEN.pow(87)..TEN.pow(90).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(87)))}OV"
        in (TEN.pow(90)..TEN.pow(93).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(90)))}NV"
        in (TEN.pow(93)..TEN.pow(96).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(93)))}Tg"
        in (TEN.pow(96)..TEN.pow(99).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(96)))}uTG"
        in (TEN.pow(99)..TEN.pow(102).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(99)))}dTg"
        in (TEN.pow(102)..TEN.pow(105).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(102)))}tTg"
        in (TEN.pow(105)..TEN.pow(108).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(105)))}qTg"
        in (TEN.pow(108)..TEN.pow(111).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(108)))}QTg"
        in (TEN.pow(111)..TEN.pow(114).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(111)))}sTg"
        in (TEN.pow(114)..TEN.pow(117).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(114)))}STg"
        in (TEN.pow(117)..TEN.pow(120).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(117)))}OTg"
        in (TEN.pow(120)..TEN.pow(123).minus(ONE)) -> "${formatter.format(this.divide(TEN.pow(120)))}NTg"
        else -> formatter.format(this)
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

inline fun <T> Iterable<T>.sumBy(selector: (T) -> BigDecimal): BigDecimal {
    var sum: BigDecimal = ZERO
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
