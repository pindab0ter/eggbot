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

fun Duration.asDaysHoursAndMinutes(compact: Boolean = false, spacing: Boolean = false): String = when (compact) {
    true -> PeriodFormatterBuilder().apply {
        appendDays()
        appendSuffix("d")
        appendSeparator(" ")
        if (spacing) appendPrefix(arrayOf("""\d\d""", """\d"""), arrayOf("", " "))
        appendHours()
        appendSuffix("h")
        appendSeparator(" ")
        printZeroAlways()
        if (spacing) appendPrefix(arrayOf("""\d\d""", """\d"""), arrayOf("", " "))
        appendMinutes()
        appendSuffix("m")
    }
    false -> PeriodFormatterBuilder().apply {
        appendDays()
        appendSuffix(" day", " days")
        appendSeparator(", ")
        if (spacing) appendPrefix(arrayOf("""\d\d""", """\d"""), arrayOf("", " "))
        appendHours()
        appendSuffix(" hour", " hours")
        appendSeparator(" and ")
        printZeroAlways()
        if (spacing) appendPrefix(arrayOf("""\d\d""", """\d"""), arrayOf("", " "))
        appendMinutes()
        appendSuffix(" minute", " minutes")
    }
}.toFormatter().withLocale(Locale.UK).print(this.toPeriod().normalizedStandard(PeriodType.dayTime()))

fun Duration.asDaysAndHours(): String = PeriodFormatterBuilder()
    .appendDays()
    .appendSuffix(" day", " days")
    .appendSeparator(", ")
    .printZeroNever()
    .appendHours()
    .appendSuffix(" hour", " hours")
    .toFormatter()
    .withLocale(Locale.UK).print(this.toPeriod().normalizedStandard(PeriodType.dayTime()))

fun Duration.asDays(compact: Boolean = false): String {
    val toPeriod = this.toPeriod()
    return when (compact) {
        true ->
            if (toPeriod.toStandardDuration().standardDays < 1L) "< 1d"
            else PeriodFormatterBuilder()
                .printZeroNever()
                .appendDays()
                .appendSuffix("d")
                .toFormatter()
                .withLocale(Locale.UK).print(toPeriod.normalizedStandard(PeriodType.dayTime()))
        false ->
            if (toPeriod.toStandardDuration().standardDays < 1L) "< 1 day"
            else PeriodFormatterBuilder()
                .printZeroNever()
                .appendDays()
                .appendSuffix(" day", " days")
                .toFormatter()
                .withLocale(Locale.UK).print(toPeriod.normalizedStandard(PeriodType.dayTime()))
    }
}

fun Duration.asHoursAndMinutes(): String = PeriodFormatterBuilder()
    .printZeroAlways()
    .appendHours()
    .appendSeparator(":")
    .minimumPrintedDigits(2)
    .appendMinutes()
    .toFormatter()
    .withLocale(Locale.UK)
    .print(this.toPeriod().normalizedStandard(PeriodType.time()))

fun DateTime.asMonthAndDay(): String = DateTimeFormatterBuilder()
    .appendMonthOfYearText()
    .appendLiteral(" ")
    .appendDayOfMonth(1)
    .toFormatter()
    .withLocale(Locale.UK)
    .print(this)

fun DateTime.asCompact(): String = DateTimeFormatterBuilder()
    .appendYear(4, 4)
    .appendLiteral("-")
    .appendMonthOfYear(2)
    .appendLiteral("-")
    .appendDayOfMonth(2)
    .toFormatter()
    .withLocale(Locale.UK)
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
    .withLocale(Locale.UK)
    .print(this)
