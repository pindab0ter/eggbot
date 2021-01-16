package nl.pindab0ter.eggbot.helpers

import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.PeriodType
import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.format.PeriodFormatterBuilder
import java.util.*
import kotlin.math.roundToLong


val ONE_YEAR: Duration get() = Duration(DateTime.now(), DateTime.now().plusYears(1))
val ONE_MINUTE: Duration = Duration.standardMinutes(1L)

fun Double.toDateTime(): DateTime = DateTime((this * 1000).roundToLong())
fun Double.toDuration(): Duration = Duration((this * 1000).roundToLong())
operator fun Duration.div(other: Duration): Double? = try {
    this.millis.toDouble() / other.millis.toDouble()
} catch (e: Exception) {
    null
}

fun Duration.formatDaysHoursAndMinutes(compact: Boolean = false, spacing: Boolean = false): String = when (compact) {
    true -> PeriodFormatterBuilder().apply {
        appendDays()
        appendSuffix("d")
        appendSeparator(" ")
        if (spacing) appendPrefix(arrayOf("""\d\d""", """\d"""), arrayOf("", " "))
        printZeroAlways()
        appendHours()
        appendSuffix("h")
        appendSeparator(" ")
        if (spacing) appendPrefix(arrayOf("""\d\d""", """\d"""), arrayOf("", " "))
        appendMinutes()
        appendSuffix("m")
    }
    false -> PeriodFormatterBuilder().apply {
        appendDays()
        appendSuffix(" day", " days")
        appendSeparator(", ")
        if (spacing) appendPrefix(arrayOf("""\d\d""", """\d"""), arrayOf("", " "))
        printZeroAlways()
        appendHours()
        appendSuffix(" hour", " hours")
        appendSeparator(" and ")
        if (spacing) appendPrefix(arrayOf("""\d\d""", """\d"""), arrayOf("", " "))
        appendMinutes()
        appendSuffix(" minute", " minutes")
    }
}.toFormatter().withLocale(Locale.UK).print(this.toPeriod().normalizedStandard(PeriodType.dayTime()))

fun Duration.formatDaysAndHours(): String = PeriodFormatterBuilder()
    .appendDays()
    .appendSuffix(" day", " days")
    .appendSeparator(", ")
    .printZeroNever()
    .appendHours()
    .appendSuffix(" hour", " hours")
    .toFormatter()
    .withLocale(Locale.UK).print(this.toPeriod().normalizedStandard(PeriodType.dayTime()))

fun Duration.formatDays(compact: Boolean = false): String = when (compact) {
    true ->
        if (toPeriod().toStandardDuration().standardDays < 1L) "< 1d"
        else PeriodFormatterBuilder()
            .printZeroNever()
            .appendDays()
            .appendSuffix("d")
            .toFormatter()
            .withLocale(Locale.UK).print(toPeriod().normalizedStandard(PeriodType.dayTime()))
    false ->
        if (toPeriod().toStandardDuration().standardDays < 1L) "< 1 day"
        else PeriodFormatterBuilder()
            .printZeroNever()
            .appendDays()
            .appendSuffix(" day", " days")
            .toFormatter()
            .withLocale(Locale.UK).print(toPeriod().normalizedStandard(PeriodType.dayTime()))
}

fun Duration.formatHourAndMinutes(): String = PeriodFormatterBuilder()
    .printZeroAlways()
    .appendHours()
    .appendSeparator(":")
    .minimumPrintedDigits(2)
    .appendMinutes()
    .toFormatter()
    .withLocale(Locale.UK)
    .print(this.toPeriod().normalizedStandard(PeriodType.time()))

fun DateTime.foramtMonthAndDay(): String = DateTimeFormatterBuilder()
    .appendMonthOfYearText()
    .appendLiteral(" ")
    .appendDayOfMonth(1)
    .toFormatter()
    .withLocale(Locale.UK)
    .print(this)

fun DateTime.formatCompact(): String = DateTimeFormatterBuilder()
    .appendYear(4, 4)
    .appendLiteral("-")
    .appendMonthOfYear(2)
    .appendLiteral("-")
    .appendDayOfMonth(2)
    .toFormatter()
    .withLocale(Locale.UK)
    .print(this)

fun DateTime.formatDayHourAndMinutes(compact: Boolean = false): String = when (compact) {
    true -> DateTimeFormatterBuilder()
        .appendDayOfWeekShortText()
        .appendLiteral(" ")
        .appendDayOfMonth(1)
        .appendLiteral(" ")
        .appendMonthOfYearShortText()
        .appendLiteral(" ")
        .appendHourOfDay(1)
        .appendLiteral(":")
        .appendMinuteOfHour(2)
        .toFormatter()
        .withLocale(Locale.UK)
        .print(this)
    false -> DateTimeFormatterBuilder()
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
}