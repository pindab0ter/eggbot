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
import java.math.BigDecimal.ONE
import java.math.BigDecimal.TEN
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale.ENGLISH
import java.util.Locale.UK
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
    .withLocale(UK)

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
    .withLocale(UK)

fun Period.asDaysHoursAndMinutes(compact: Boolean = false): String = when (compact) {
    true ->
        if (toStandardDuration().standardSeconds < 60L) "< 1m"
        else shortDaysHoursAndMinutesFormatter.print(this.normalizedStandard(PeriodType.dayTime()))
    false ->
        if (toStandardDuration().standardSeconds < 60L) "< 1 minute"
        else longDaysHoursAndMinutesFormatter.print(this.normalizedStandard(PeriodType.dayTime()))
}


private val longDaysFormatter: PeriodFormatter = PeriodFormatterBuilder()
    .printZeroNever()
    .appendDays()
    .appendSuffix(" day", " days")
    .toFormatter()
    .withLocale(UK)

private val shortDaysFormatter: PeriodFormatter = PeriodFormatterBuilder()
    .printZeroNever()
    .appendDays()
    .appendSuffix("d")
    .toFormatter()
    .withLocale(UK)


fun Period.asDays(compact: Boolean = false): String = when (compact) {
    true ->
        if (toStandardDuration().standardDays < 1L) "< 1d"
        else shortDaysFormatter.print(this.normalizedStandard(PeriodType.dayTime()))
    false ->
        if (toStandardDuration().standardDays < 1L) "< 1 day"
        else longDaysFormatter.print(this.normalizedStandard(PeriodType.dayTime()))
}

fun Duration.asDaysHoursAndMinutes(compact: Boolean = false): String = this.toPeriod().asDaysHoursAndMinutes(compact)

fun Duration.asDays(compact: Boolean = false): String = this.toPeriod().asDays(compact)

fun DateTime.asMonthAndDay(): String = DateTimeFormatterBuilder()
    .appendMonthOfYearText()
    .appendLiteral(" ")
    .appendDayOfMonth(1)
    .toFormatter()
    .withLocale(UK)
    .print(this)

fun DateTime.asCompact(): String = DateTimeFormatterBuilder()
    .appendYear(4, 4)
    .appendLiteral("-")
    .appendMonthOfYear(2)
    .appendLiteral("-")
    .appendDayOfMonth(2)
    .toFormatter()
    .withLocale(UK)
    .print(this)

fun DateTime.asDaysHoursAndMinutes(): String = DateTimeFormatterBuilder()
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
    .withLocale(UK)
    .print(this)

val decimalFormat = DecimalFormat(",##0.00", DecimalFormatSymbols.getInstance(ENGLISH))
val integerFormat = DecimalFormat(",##0", DecimalFormatSymbols.getInstance(ENGLISH))

fun Int.formatInteger(): String = integerFormat.format(this)
fun Long.formatInteger(): String = integerFormat.format(this)
fun BigDecimal.formatInteger(): String = integerFormat.format(this)

fun BigDecimal.formatIllions(rounded: Boolean = false): String {
    val f = if (rounded) integerFormat else decimalFormat
    return when (this) {
        in (TEN.pow(3)..TEN.pow(6) - ONE) -> f.format(this / TEN.pow(3)) + "k"
        in (TEN.pow(6)..TEN.pow(9) - ONE) -> f.format(this / TEN.pow(6)) + "M"
        in (TEN.pow(9)..TEN.pow(12) - ONE) -> f.format(this / TEN.pow(9)) + "B"
        in (TEN.pow(12)..TEN.pow(15) - ONE) -> f.format(this / TEN.pow(12)) + "T"
        in (TEN.pow(15)..TEN.pow(18) - ONE) -> f.format(this / TEN.pow(15)) + "q"
        in (TEN.pow(18)..TEN.pow(21) - ONE) -> f.format(this / TEN.pow(18)) + "Q"
        in (TEN.pow(21)..TEN.pow(24) - ONE) -> f.format(this / TEN.pow(21)) + "s"
        in (TEN.pow(24)..TEN.pow(27) - ONE) -> f.format(this / TEN.pow(24)) + "S"
        in (TEN.pow(27)..TEN.pow(30) - ONE) -> f.format(this / TEN.pow(27)) + "O"
        in (TEN.pow(30)..TEN.pow(33) - ONE) -> f.format(this / TEN.pow(30)) + "N"
        in (TEN.pow(33)..TEN.pow(36) - ONE) -> f.format(this / TEN.pow(33)) + "D"
        in (TEN.pow(36)..TEN.pow(39) - ONE) -> f.format(this / TEN.pow(36)) + "uD"
        in (TEN.pow(39)..TEN.pow(42) - ONE) -> f.format(this / TEN.pow(39)) + "dD"
        in (TEN.pow(42)..TEN.pow(45) - ONE) -> f.format(this / TEN.pow(42)) + "tD"
        in (TEN.pow(45)..TEN.pow(48) - ONE) -> f.format(this / TEN.pow(45)) + "qD"
        in (TEN.pow(48)..TEN.pow(51) - ONE) -> f.format(this / TEN.pow(48)) + "QD"
        in (TEN.pow(51)..TEN.pow(54) - ONE) -> f.format(this / TEN.pow(51)) + "sD"
        in (TEN.pow(54)..TEN.pow(57) - ONE) -> f.format(this / TEN.pow(54)) + "SD"
        in (TEN.pow(57)..TEN.pow(60) - ONE) -> f.format(this / TEN.pow(57)) + "OD"
        in (TEN.pow(60)..TEN.pow(63) - ONE) -> f.format(this / TEN.pow(60)) + "ND"
        in (TEN.pow(63)..TEN.pow(66) - ONE) -> f.format(this / TEN.pow(63)) + "V"
        in (TEN.pow(66)..TEN.pow(69) - ONE) -> f.format(this / TEN.pow(66)) + "uV"
        in (TEN.pow(69)..TEN.pow(72) - ONE) -> f.format(this / TEN.pow(69)) + "dV"
        in (TEN.pow(72)..TEN.pow(75) - ONE) -> f.format(this / TEN.pow(72)) + "tV"
        in (TEN.pow(75)..TEN.pow(78) - ONE) -> f.format(this / TEN.pow(75)) + "qV"
        in (TEN.pow(78)..TEN.pow(81) - ONE) -> f.format(this / TEN.pow(78)) + "QV"
        in (TEN.pow(81)..TEN.pow(84) - ONE) -> f.format(this / TEN.pow(81)) + "sV"
        in (TEN.pow(84)..TEN.pow(87) - ONE) -> f.format(this / TEN.pow(84)) + "SV"
        in (TEN.pow(87)..TEN.pow(90) - ONE) -> f.format(this / TEN.pow(87)) + "OV"
        in (TEN.pow(90)..TEN.pow(93) - ONE) -> f.format(this / TEN.pow(90)) + "NV"
        in (TEN.pow(93)..TEN.pow(96) - ONE) -> f.format(this / TEN.pow(93)) + "Tg"
        in (TEN.pow(96)..TEN.pow(99) - ONE) -> f.format(this / TEN.pow(96)) + "uTG"
        in (TEN.pow(99)..TEN.pow(102) - ONE) -> f.format(this / TEN.pow(99)) + "dTg"
        in (TEN.pow(102)..TEN.pow(105) - ONE) -> f.format(this / TEN.pow(102)) + "tTg"
        in (TEN.pow(105)..TEN.pow(108) - ONE) -> f.format(this / TEN.pow(105)) + "qTg"
        in (TEN.pow(108)..TEN.pow(111) - ONE) -> f.format(this / TEN.pow(108)) + "QTg"
        in (TEN.pow(111)..TEN.pow(114) - ONE) -> f.format(this / TEN.pow(111)) + "sTg"
        in (TEN.pow(114)..TEN.pow(117) - ONE) -> f.format(this / TEN.pow(114)) + "STg"
        in (TEN.pow(117)..TEN.pow(120) - ONE) -> f.format(this / TEN.pow(117)) + "OTg"
        in (TEN.pow(120)..TEN.pow(123) - ONE) -> f.format(this / TEN.pow(120)) + "NTg"
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
