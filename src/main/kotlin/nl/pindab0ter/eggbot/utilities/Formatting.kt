package nl.pindab0ter.eggbot.utilities

import com.auxbrain.ei.EggInc
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.Period
import org.joda.time.PeriodType
import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.*
import kotlin.math.roundToLong

val EggInc.Egg.formattedName: String
    get() = name
        .toLowerCase()
        .split('_')
        .joinToString(" ", transform = String::capitalize)

fun Long.toDuration(): Duration = Duration((this * 1000))
fun Double.toDateTime(): DateTime = DateTime((this * 1000).roundToLong())
fun Double.toDuration(): Duration = Duration((this * 1000).roundToLong())
fun Double.asPercentage(): String = NumberFormat.getPercentInstance().format(this)

private val longDaysHoursAndMinutesFormatter: PeriodFormatter = PeriodFormatterBuilder()
    .printZeroNever()
    .appendDays()
    .appendSuffix(" day", " days")
    .appendSeparator(", ")
    .appendHours()
    .appendSuffix(" hour", " hours")
    .appendSeparator(" and ")
    .appendMinutes()
    .appendSuffix(" minute", " minutes")
    .toFormatter()

private val shortDaysHoursAndMinutesFormatter: PeriodFormatter = PeriodFormatterBuilder()
    .printZeroNever()
    .appendDays()
    .appendSuffix("d")
    .appendSeparator(" ")
    .appendHours()
    .appendSuffix("h")
    .appendSeparator(" ")
    .appendMinutes()
    .appendSuffix("m")
    .toFormatter()

fun Period.asDayHoursAndMinutes(compact: Boolean = false): String =
    (if (compact) shortDaysHoursAndMinutesFormatter else longDaysHoursAndMinutesFormatter)
        .withLocale(Locale.UK)
        .print(this.normalizedStandard(PeriodType.dayTime()))

fun Duration.asDayHoursAndMinutes(compact: Boolean = false): String = this.toPeriod().asDayHoursAndMinutes(compact)

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

val decimalFormat = DecimalFormat(",##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
val integerFormat = DecimalFormat(",##0", DecimalFormatSymbols.getInstance(Locale.ENGLISH))

fun Int.formatInteger(): String = integerFormat.format(this)
fun Long.formatInteger(): String = integerFormat.format(this)
fun BigDecimal.formatInteger(): String = integerFormat.format(this)

fun BigDecimal.formatIllions(rounded: Boolean = false): String {
    val f = if (rounded) integerFormat else decimalFormat
    return when (this) {
        in (BigDecimal.TEN.pow(3)..BigDecimal.TEN.pow(6) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(3)) + "k"
        in (BigDecimal.TEN.pow(6)..BigDecimal.TEN.pow(9) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(6)) + "M"
        in (BigDecimal.TEN.pow(9)..BigDecimal.TEN.pow(12) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(9)) + "B"
        in (BigDecimal.TEN.pow(12)..BigDecimal.TEN.pow(15) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(12)) + "T"
        in (BigDecimal.TEN.pow(15)..BigDecimal.TEN.pow(18) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(15)) + "q"
        in (BigDecimal.TEN.pow(18)..BigDecimal.TEN.pow(21) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(18)) + "Q"
        in (BigDecimal.TEN.pow(21)..BigDecimal.TEN.pow(24) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(21)) + "s"
        in (BigDecimal.TEN.pow(24)..BigDecimal.TEN.pow(27) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(24)) + "S"
        in (BigDecimal.TEN.pow(27)..BigDecimal.TEN.pow(30) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(27)) + "O"
        in (BigDecimal.TEN.pow(30)..BigDecimal.TEN.pow(33) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(30)) + "N"
        in (BigDecimal.TEN.pow(33)..BigDecimal.TEN.pow(36) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(33)) + "D"
        in (BigDecimal.TEN.pow(36)..BigDecimal.TEN.pow(39) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(36)) + "uD"
        in (BigDecimal.TEN.pow(39)..BigDecimal.TEN.pow(42) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(39)) + "dD"
        in (BigDecimal.TEN.pow(42)..BigDecimal.TEN.pow(45) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(42)) + "tD"
        in (BigDecimal.TEN.pow(45)..BigDecimal.TEN.pow(48) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(45)) + "qD"
        in (BigDecimal.TEN.pow(48)..BigDecimal.TEN.pow(51) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(48)) + "QD"
        in (BigDecimal.TEN.pow(51)..BigDecimal.TEN.pow(54) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(51)) + "sD"
        in (BigDecimal.TEN.pow(54)..BigDecimal.TEN.pow(57) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(54)) + "SD"
        in (BigDecimal.TEN.pow(57)..BigDecimal.TEN.pow(60) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(57)) + "OD"
        in (BigDecimal.TEN.pow(60)..BigDecimal.TEN.pow(63) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(60)) + "ND"
        in (BigDecimal.TEN.pow(63)..BigDecimal.TEN.pow(66) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(63)) + "V"
        in (BigDecimal.TEN.pow(66)..BigDecimal.TEN.pow(69) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(66)) + "uV"
        in (BigDecimal.TEN.pow(69)..BigDecimal.TEN.pow(72) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(69)) + "dV"
        in (BigDecimal.TEN.pow(72)..BigDecimal.TEN.pow(75) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(72)) + "tV"
        in (BigDecimal.TEN.pow(75)..BigDecimal.TEN.pow(78) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(75)) + "qV"
        in (BigDecimal.TEN.pow(78)..BigDecimal.TEN.pow(81) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(78)) + "QV"
        in (BigDecimal.TEN.pow(81)..BigDecimal.TEN.pow(84) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(81)) + "sV"
        in (BigDecimal.TEN.pow(84)..BigDecimal.TEN.pow(87) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(84)) + "SV"
        in (BigDecimal.TEN.pow(87)..BigDecimal.TEN.pow(90) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(87)) + "OV"
        in (BigDecimal.TEN.pow(90)..BigDecimal.TEN.pow(93) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(90)) + "NV"
        in (BigDecimal.TEN.pow(93)..BigDecimal.TEN.pow(96) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(93)) + "Tg"
        in (BigDecimal.TEN.pow(96)..BigDecimal.TEN.pow(99) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(96)) + "uTG"
        in (BigDecimal.TEN.pow(99)..BigDecimal.TEN.pow(102) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(99)) + "dTg"
        in (BigDecimal.TEN.pow(102)..BigDecimal.TEN.pow(105) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(102)) + "tTg"
        in (BigDecimal.TEN.pow(105)..BigDecimal.TEN.pow(108) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(105)) + "qTg"
        in (BigDecimal.TEN.pow(108)..BigDecimal.TEN.pow(111) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(108)) + "QTg"
        in (BigDecimal.TEN.pow(111)..BigDecimal.TEN.pow(114) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(111)) + "sTg"
        in (BigDecimal.TEN.pow(114)..BigDecimal.TEN.pow(117) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(114)) + "STg"
        in (BigDecimal.TEN.pow(117)..BigDecimal.TEN.pow(120) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(117)) + "OTg"
        in (BigDecimal.TEN.pow(120)..BigDecimal.TEN.pow(123) - BigDecimal.ONE) -> f.format(this / BigDecimal.TEN.pow(120)) + "NTg"
        else -> integerFormat.format(this)
    }
}

// Padding

fun paddingCharacters(current: Any, longest: Any, character: String = " "): String {
    val currentLength = current.toString().length
    val longestLength = longest.toString().length

    require(longestLength >= currentLength) { "Longest's length ($longestLength) must be longer than current's length ($currentLength)" }

    return character.repeat(longestLength - currentLength)
}

fun <T : Any> paddingCharacters(
    current: T,
    containsLongest: Iterable<T>,
    character: String = " "
): String = paddingCharacters(current, containsLongest.maxBy { it.toString().length }!!, character)

fun <T : Any> StringBuilder.appendPaddingCharacters(
    current: T,
    longest: T,
    character: String = " "
): StringBuilder = append(paddingCharacters(current, longest, character))

fun <T : Any> StringBuilder.appendPaddingCharacters(
    current: T,
    containsLongest: Iterable<T>,
    character: String = " "
): StringBuilder = append(paddingCharacters(current, containsLongest.plus(current), character))
