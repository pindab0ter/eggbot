package nl.pindab0ter.eggbot.helpers

import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.Period
import org.joda.time.PeriodType
import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.format.PeriodFormatter
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
    .withLocale(Locale.UK)

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
    .withLocale(Locale.UK)

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
    .withLocale(Locale.UK)

private val shortDaysFormatter: PeriodFormatter = PeriodFormatterBuilder()
    .printZeroNever()
    .appendDays()
    .appendSuffix("d")
    .toFormatter()
    .withLocale(Locale.UK)


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
