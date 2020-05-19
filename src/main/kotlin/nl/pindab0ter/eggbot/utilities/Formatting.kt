package nl.pindab0ter.eggbot.utilities

import com.auxbrain.ei.Egg
import nl.pindab0ter.eggbot.utilities.NumberFormatter.*
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.Period
import org.joda.time.PeriodType
import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder
import java.math.BigDecimal
import java.math.BigDecimal.*
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale.ENGLISH
import java.util.Locale.UK
import kotlin.math.roundToInt
import kotlin.math.roundToLong

fun Double.toDateTime(): DateTime = DateTime((this * 1000).roundToLong())
fun Double.toDuration(): Duration = Duration((this * 1000).roundToLong())

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

fun Period.asHoursAndMinutes(): String = PeriodFormatterBuilder()
    .printZeroNever()
    .appendSeparator(" ")
    .appendHours()
    .appendSuffix("h")
    .appendSeparator(" ")
    .appendMinutes()
    .appendSuffix("m")
    .toFormatter()
    .withLocale(UK)
    .print(this.toStandardHours())

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

fun Duration.asHoursAndMinutes(): String = this.toPeriod().asHoursAndMinutes()

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

enum class NumberFormatter {
    /** Format as integer */
    INTEGER {
        override fun format(number: Number): String =
            DecimalFormat(",##0", DecimalFormatSymbols.getInstance(ENGLISH)).format(number)
    },

    /** Format to three decimal places */
    DECIMALS {
        override fun format(number: Number): String =
            DecimalFormat(",##0.00", DecimalFormatSymbols.getInstance(ENGLISH)).format(number)
    },

    /** Format to as little decimal places as needed, with a maximum of three */
    OPTIONAL_DECIMALS {
        override fun format(number: Number): String =
            DecimalFormat(",##0.##", DecimalFormatSymbols.getInstance(ENGLISH)).format(number)
    };

    abstract fun format(number: Number): String
}

fun Int.formatInteger(): String = INTEGER.format(this)
fun Long.formatInteger(): String = INTEGER.format(this)
fun BigDecimal.formatInteger(): String = INTEGER.format(this)

fun BigDecimal.asIllions(formatter: NumberFormatter = DECIMALS, shortened: Boolean = true): String {
    return when (this) {
        in (TEN.pow(3)..TEN.pow(6) - ONE) -> formatter.format(this / TEN.pow(3)) + if (shortened) "k" else " Kilo"
        in (TEN.pow(6)..TEN.pow(9) - ONE) -> formatter.format(this / TEN.pow(6)) + if (shortened) "M" else " Million"
        in (TEN.pow(9)..TEN.pow(12) - ONE) -> formatter.format(this / TEN.pow(9)) + if (shortened) "B" else " Billion"
        in (TEN.pow(12)..TEN.pow(15) - ONE) -> formatter.format(this / TEN.pow(12)) + if (shortened) "T" else " Trillion"
        in (TEN.pow(15)..TEN.pow(18) - ONE) -> formatter.format(this / TEN.pow(15)) + if (shortened) "q" else " Quadrillion"
        in (TEN.pow(18)..TEN.pow(21) - ONE) -> formatter.format(this / TEN.pow(18)) + if (shortened) "Q" else " Quintillion"
        in (TEN.pow(21)..TEN.pow(24) - ONE) -> formatter.format(this / TEN.pow(21)) + if (shortened) "s" else " Sextillion"
        in (TEN.pow(24)..TEN.pow(27) - ONE) -> formatter.format(this / TEN.pow(24)) + if (shortened) "S" else " Septillion"
        in (TEN.pow(27)..TEN.pow(30) - ONE) -> formatter.format(this / TEN.pow(27)) + if (shortened) "O" else " Octillion"
        in (TEN.pow(30)..TEN.pow(33) - ONE) -> formatter.format(this / TEN.pow(30)) + if (shortened) "N" else " Nonillion"
        in (TEN.pow(33)..TEN.pow(36) - ONE) -> formatter.format(this / TEN.pow(33)) + if (shortened) "D" else " Decillion"
        in (TEN.pow(36)..TEN.pow(39) - ONE) -> formatter.format(this / TEN.pow(36)) + if (shortened) "uD" else " Undecillion"
        in (TEN.pow(39)..TEN.pow(42) - ONE) -> formatter.format(this / TEN.pow(39)) + if (shortened) "dD" else " Duodecillion"
        in (TEN.pow(42)..TEN.pow(45) - ONE) -> formatter.format(this / TEN.pow(42)) + if (shortened) "tD" else " Tredecillion"
        in (TEN.pow(45)..TEN.pow(48) - ONE) -> formatter.format(this / TEN.pow(45)) + if (shortened) "qD" else " Quattuordecillion"
        in (TEN.pow(48)..TEN.pow(51) - ONE) -> formatter.format(this / TEN.pow(48)) + if (shortened) "QD" else " Quindecillion"
        in (TEN.pow(51)..TEN.pow(54) - ONE) -> formatter.format(this / TEN.pow(51)) + if (shortened) "sD" else " Sexdecillion"
        in (TEN.pow(54)..TEN.pow(57) - ONE) -> formatter.format(this / TEN.pow(54)) + if (shortened) "SD" else " Septdecillion"
        in (TEN.pow(57)..TEN.pow(60) - ONE) -> formatter.format(this / TEN.pow(57)) + if (shortened) "OD" else " Octodecillion"
        in (TEN.pow(60)..TEN.pow(63) - ONE) -> formatter.format(this / TEN.pow(60)) + if (shortened) "ND" else " Novemdecillion"
        in (TEN.pow(63)..TEN.pow(66) - ONE) -> formatter.format(this / TEN.pow(63)) + if (shortened) "V" else " Vigintillion"
        in (TEN.pow(66)..TEN.pow(69) - ONE) -> formatter.format(this / TEN.pow(66)) + if (shortened) "uV" else " Unvigintillion"
        in (TEN.pow(69)..TEN.pow(72) - ONE) -> formatter.format(this / TEN.pow(69)) + if (shortened) "dV" else " Duovigintillion"
        in (TEN.pow(72)..TEN.pow(75) - ONE) -> formatter.format(this / TEN.pow(72)) + if (shortened) "tV" else " Trevigintillion"
        in (TEN.pow(75)..TEN.pow(78) - ONE) -> formatter.format(this / TEN.pow(75)) + if (shortened) "qV" else " Quattuorvigintillion"
        in (TEN.pow(78)..TEN.pow(81) - ONE) -> formatter.format(this / TEN.pow(78)) + if (shortened) "QV" else " Quinvigintillion"
        in (TEN.pow(81)..TEN.pow(84) - ONE) -> formatter.format(this / TEN.pow(81)) + if (shortened) "sV" else " Sexvigintillion"
        in (TEN.pow(84)..TEN.pow(87) - ONE) -> formatter.format(this / TEN.pow(84)) + if (shortened) "SV" else " Septenvigintillion"
        in (TEN.pow(87)..TEN.pow(90) - ONE) -> formatter.format(this / TEN.pow(87)) + if (shortened) "OV" else " Octovigintillion"
        in (TEN.pow(90)..TEN.pow(93) - ONE) -> formatter.format(this / TEN.pow(90)) + if (shortened) "NV" else " Novemvigintillion"
        in (TEN.pow(93)..TEN.pow(96) - ONE) -> formatter.format(this / TEN.pow(93)) + if (shortened) "Tg" else " Trigintillion"
        in (TEN.pow(96)..TEN.pow(99) - ONE) -> formatter.format(this / TEN.pow(96)) + if (shortened) "uTG" else " Untrigintillion"
        in (TEN.pow(99)..TEN.pow(102) - ONE) -> formatter.format(this / TEN.pow(99)) + if (shortened) "dTg" else " Duotrigintillion"
        in (TEN.pow(102)..TEN.pow(105) - ONE) -> formatter.format(this / TEN.pow(102)) + if (shortened) "tTg" else " Tretrigintillion"
        in (TEN.pow(105)..TEN.pow(108) - ONE) -> formatter.format(this / TEN.pow(105)) + if (shortened) "qTg" else " Quattuortrigintillion"
        in (TEN.pow(108)..TEN.pow(111) - ONE) -> formatter.format(this / TEN.pow(108)) + if (shortened) "QTg" else " Quintrigintillion"
        in (TEN.pow(111)..TEN.pow(114) - ONE) -> formatter.format(this / TEN.pow(111)) + if (shortened) "sTg" else " Sextrigintillion"
        in (TEN.pow(114)..TEN.pow(117) - ONE) -> formatter.format(this / TEN.pow(114)) + if (shortened) "STg" else " Septentrigintillion"
        in (TEN.pow(117)..TEN.pow(120) - ONE) -> formatter.format(this / TEN.pow(117)) + if (shortened) "OTg" else " Octotrigintillion"
        in (TEN.pow(120)..TEN.pow(123) - ONE) -> formatter.format(this / TEN.pow(120)) + if (shortened) "NTg" else " Novemtrigintillion"
        else -> INTEGER.format(this)
    }
}

fun BigDecimal.asFarmerRole(shortened: Boolean = false): String = when (this) {
    in (ZERO..TEN.pow(3) - ONE) -> "Farmer"
    in (TEN.pow(3)..TEN.pow(4) - ONE) -> "Farmer II"
    in (TEN.pow(4)..TEN.pow(5) - ONE) -> "Farmer III"
    in (TEN.pow(5)..TEN.pow(6) - ONE) -> "Kilo${if (shortened) "" else "farmer"}"
    in (TEN.pow(6)..TEN.pow(7) - ONE) -> "Kilo${if (shortened) "" else "farmer"} II"
    in (TEN.pow(7)..TEN.pow(8) - ONE) -> "Kilo${if (shortened) "" else "farmer"} III"
    in (TEN.pow(8)..TEN.pow(9) - ONE) -> "Mega${if (shortened) "" else "farmer"}"
    in (TEN.pow(9)..TEN.pow(10) - ONE) -> "Mega${if (shortened) "" else "farmer"} II"
    in (TEN.pow(10)..TEN.pow(11) - ONE) -> "Mega${if (shortened) "" else "farmer"} III"
    in (TEN.pow(11)..TEN.pow(12) - ONE) -> "Giga${if (shortened) "" else "farmer"}"
    in (TEN.pow(12)..TEN.pow(13) - ONE) -> "Giga${if (shortened) "" else "farmer"} II"
    in (TEN.pow(13)..TEN.pow(14) - ONE) -> "Giga${if (shortened) "" else "farmer"} III"
    in (TEN.pow(14)..TEN.pow(15) - ONE) -> "Tera${if (shortened) "" else "farmer"}"
    in (TEN.pow(15)..TEN.pow(16) - ONE) -> "Tera${if (shortened) "" else "farmer"} II"
    in (TEN.pow(16)..TEN.pow(17) - ONE) -> "Tera${if (shortened) "" else "farmer"} III"
    in (TEN.pow(17)..TEN.pow(18) - ONE) -> "Peta${if (shortened) "" else "farmer"}"
    in (TEN.pow(18)..TEN.pow(19) - ONE) -> "Peta${if (shortened) "" else "farmer"} II"
    in (TEN.pow(19)..TEN.pow(20) - ONE) -> "Peta${if (shortened) "" else "farmer"} III"
    in (TEN.pow(20)..TEN.pow(21) - ONE) -> "Exa${if (shortened) "" else "farmer"}"
    in (TEN.pow(21)..TEN.pow(22) - ONE) -> "Exa${if (shortened) "" else "farmer"} II"
    in (TEN.pow(22)..TEN.pow(23) - ONE) -> "Exa${if (shortened) "" else "farmer"} III"
    in (TEN.pow(23)..TEN.pow(24) - ONE) -> "Zetta${if (shortened) "" else "farmer"}"
    in (TEN.pow(24)..TEN.pow(25) - ONE) -> "Zetta${if (shortened) "" else "farmer"} II"
    in (TEN.pow(25)..TEN.pow(26) - ONE) -> "Zetta${if (shortened) "" else "farmer"} III"
    in (TEN.pow(26)..TEN.pow(27) - ONE) -> "Yoda${if (shortened) "" else "farmer"}"
    in (TEN.pow(27)..TEN.pow(28) - ONE) -> "Yoda${if (shortened) "" else "farmer"} II"
    in (TEN.pow(28)..TEN.pow(29) - ONE) -> "Yoda${if (shortened) "" else "farmer"} III"
    in (TEN.pow(29)..TEN.pow(30) - ONE) -> "Xenna${if (shortened) "" else "farmer"}"
    in (TEN.pow(30)..TEN.pow(31) - ONE) -> "Xenna${if (shortened) "" else "farmer"} II"
    in (TEN.pow(31)..TEN.pow(32) - ONE) -> "Xenna${if (shortened) "" else "farmer"} III"
    in (TEN.pow(32)..TEN.pow(33) - ONE) -> "Wecca${if (shortened) "" else "farmer"}"
    in (TEN.pow(33)..TEN.pow(34) - ONE) -> "Wecca${if (shortened) "" else "farmer"} II"
    in (TEN.pow(34)..TEN.pow(35) - ONE) -> "Wecca${if (shortened) "" else "farmer"} III"
    in (TEN.pow(35)..TEN.pow(36) - ONE) -> "Venda${if (shortened) "" else "farmer"}"
    in (TEN.pow(36)..TEN.pow(37) - ONE) -> "Venda${if (shortened) "" else "farmer"} II"
    in (TEN.pow(37)..TEN.pow(38) - ONE) -> "Venda${if (shortened) "" else "farmer"} III"
    else -> "Unknown"
}

// Padding

// TODO: Replace with String.rightPad?

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

fun drawProgressBar(
    current: Int,
    total: Int,
    width: Int = 30,
    showSteps: Boolean = true,
    showPercentage: Boolean = true
): String {
    val percentage = (current.toDouble() / total.toDouble() * 100.0).roundToInt()
    val full = (current.toDouble() / total.toDouble() * width.toDouble()).roundToInt()
    val empty = width - full

    return StringBuilder().apply {
        append("`")
        append("▓".repeat(full))
        append("░".repeat(empty))
        if (showSteps) append(" ${paddingCharacters(current, total)}$current/$total")
        if (showPercentage) append(" ($percentage%)")
        append("`")
    }.toString()
}
