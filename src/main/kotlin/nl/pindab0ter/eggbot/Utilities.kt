package nl.pindab0ter.eggbot

import net.dv8tion.jda.core.entities.Message
import nl.pindab0ter.eggbot.auxbrain.EggInc
import nl.pindab0ter.eggbot.commands.PREFIX
import org.joda.time.DateTime
import org.joda.time.Period
import org.joda.time.PeriodType
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder
import kotlin.math.roundToLong

class MissingEnvironmentVariableException(message: String? = null) : Exception(message)

val Message.isCommand: Boolean
    get() = contentDisplay?.startsWith(PREFIX) == true && !author.isBot

val Message.command: String?
    get() = contentDisplay?.takeIf { isCommand }
        ?.removePrefix(PREFIX)
        ?.split(' ')
        ?.first()

val Message.arguments: List<String>?
    get() = contentDisplay?.takeIf { isCommand }
        ?.removePrefix(PREFIX)
        ?.split(' ')
        ?.drop(1)

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