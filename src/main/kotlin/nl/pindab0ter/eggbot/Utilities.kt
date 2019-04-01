package nl.pindab0ter.eggbot

import com.auxbrain.ei.EggInc
import org.joda.time.DateTime
import org.joda.time.Period
import org.joda.time.PeriodType
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder
import kotlin.math.roundToLong

val EggInc.Egg.formattedName: String
    get() = name
        .toLowerCase()
        .split('_')
        .joinToString(" ", transform = String::capitalize)

fun Double.toDateTime(): DateTime = DateTime((this * 1000).roundToLong())
fun Double.toPeriod(): Period = Period((this * 1000).roundToLong()).normalizedStandard(PeriodType.days())

val daysAndHours: PeriodFormatter = PeriodFormatterBuilder()
    .printZeroNever()
    .appendDays()
    .appendSuffix(" day", " days")
    .appendSeparator(" and ")
    .appendHours()
    .appendSuffix(" hour", " hours")
    .toFormatter()