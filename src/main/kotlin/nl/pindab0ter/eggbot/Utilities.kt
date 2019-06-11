package nl.pindab0ter.eggbot

import com.auxbrain.ei.EggInc
import com.github.kittinunf.fuel.core.Body
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.dv8tion.jda.core.entities.ChannelType
import nl.pindab0ter.eggbot.jda.commandClient
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.Period
import org.joda.time.PeriodType
import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder
import java.math.BigDecimal
import java.math.BigDecimal.*
import java.math.MathContext.DECIMAL32
import java.math.MathContext.UNLIMITED
import java.math.RoundingMode.FLOOR
import java.math.RoundingMode.HALF_UP
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

fun Long.toDuration(): Duration = Duration((this * 1000))
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

private val hoursMinutesAndSecondsFormatter = PeriodFormatterBuilder()
    .printZeroNever()
    .appendHours()
    .appendSuffix("h")
    .appendSeparator(" ")
    .appendMinutes()
    .appendSuffix("m")
    .appendSeparator(" ")
    .appendSeconds()
    .appendSuffix("s")
    .toFormatter()

// TODO: Add compact option
fun Period.asHoursMinutesAndSeconds(): String = hoursMinutesAndSecondsFormatter
    .withLocale(Locale.UK)
    .print(this.normalizedStandard(PeriodType.time()))

fun Duration.asDayHoursAndMinutes(compact: Boolean = false): String = this.toPeriod().asDayHoursAndMinutes(compact)
fun Duration.asHoursMinutesAndSeconds(): String = this.toPeriod().asHoursMinutesAndSeconds()

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
fun Long.formatDecimal(): String = decimalFormat.format(this)
fun Long.formatInteger(): String = integerFormat.format(this)
fun BigDecimal.formatDecimal(): String = decimalFormat.format(this.round(UNLIMITED))
fun BigDecimal.formatInteger(): String = integerFormat.format(this)

fun Double.formatIllions(rounded: Boolean = false): String {
    val f = if (rounded) integerFormat else decimalFormat
    return when (this) {
        in (1.0e003..1.0e006 - 1) -> f.format(this / 1e003) + "k"
        in (1.0e006..1.0e009 - 1) -> f.format(this / 1e006) + "M"
        in (1.0e009..1.0e012 - 1) -> f.format(this / 1e009) + "B"
        in (1.0e012..1.0e015 - 1) -> f.format(this / 1e012) + "T"
        in (1.0e015..1.0e018 - 1) -> f.format(this / 1e015) + "q"
        in (1.0e018..1.0e021 - 1) -> f.format(this / 1e018) + "Q"
        in (1.0e021..1.0e024 - 1) -> f.format(this / 1e021) + "s"
        in (1.0e024..1.0e027 - 1) -> f.format(this / 1e024) + "S"
        in (1.0e027..1.0e030 - 1) -> f.format(this / 1e027) + "O"
        in (1.0e030..1.0e033 - 1) -> f.format(this / 1e030) + "N"
        in (1.0e033..1.0e036 - 1) -> f.format(this / 1e033) + "D"
        in (1.0e036..1.0e039 - 1) -> f.format(this / 1e036) + "uD"
        in (1.0e039..1.0e042 - 1) -> f.format(this / 1e039) + "dD"
        in (1.0e042..1.0e045 - 1) -> f.format(this / 1e042) + "tD"
        in (1.0e045..1.0e048 - 1) -> f.format(this / 1e045) + "qD"
        in (1.0e048..1.0e051 - 1) -> f.format(this / 1e048) + "QD"
        in (1.0e051..1.0e054 - 1) -> f.format(this / 1e051) + "sD"
        in (1.0e054..1.0e057 - 1) -> f.format(this / 1e054) + "SD"
        in (1.0e057..1.0e060 - 1) -> f.format(this / 1e057) + "OD"
        in (1.0e060..1.0e063 - 1) -> f.format(this / 1e060) + "ND"
        in (1.0e063..1.0e066 - 1) -> f.format(this / 1e063) + "V"
        in (1.0e066..1.0e069 - 1) -> f.format(this / 1e066) + "uV"
        in (1.0e069..1.0e072 - 1) -> f.format(this / 1e069) + "dV"
        in (1.0e072..1.0e075 - 1) -> f.format(this / 1e072) + "tV"
        in (1.0e075..1.0e078 - 1) -> f.format(this / 1e075) + "qV"
        in (1.0e078..1.0e081 - 1) -> f.format(this / 1e078) + "QV"
        in (1.0e081..1.0e084 - 1) -> f.format(this / 1e081) + "sV"
        in (1.0e084..1.0e087 - 1) -> f.format(this / 1e084) + "SV"
        in (1.0e087..1.0e090 - 1) -> f.format(this / 1e087) + "OV"
        in (1.0e090..1.0e093 - 1) -> f.format(this / 1e090) + "NV"
        in (1.0e093..1.0e096 - 1) -> f.format(this / 1e093) + "Tg"
        in (1.0e096..1.0e099 - 1) -> f.format(this / 1e096) + "uTG"
        in (1.0e099..1.0e102 - 1) -> f.format(this / 1e099) + "dTg"
        in (1.0e102..1.0e105 - 1) -> f.format(this / 1e102) + "tTg"
        in (1.0e105..1.0e108 - 1) -> f.format(this / 1e105) + "qTg"
        in (1.0e108..1.0e111 - 1) -> f.format(this / 1e108) + "QTg"
        in (1.0e111..1.0e114 - 1) -> f.format(this / 1e111) + "sTg"
        in (1.0e114..1.0e117 - 1) -> f.format(this / 1e114) + "STg"
        in (1.0e117..1.0e120 - 1) -> f.format(this / 1e117) + "OTg"
        in (1.0e120..1.0e123 - 1) -> f.format(this / 1e120) + "NTg"
        else -> f.format(this)
    }
}

fun BigDecimal.formatIllions(rounded: Boolean = false): String {
    val f = if (rounded) integerFormat else decimalFormat
    return when (this) {
        in (TEN.pow(3)..TEN.pow(6) - ONE) -> f.format(this.divide(TEN.pow(3), DECIMAL32)) + "k"
        in (TEN.pow(6)..TEN.pow(9) - ONE) -> f.format(this.divide(TEN.pow(6), DECIMAL32)) + "M"
        in (TEN.pow(9)..TEN.pow(12) - ONE) -> f.format(this.divide(TEN.pow(9), DECIMAL32)) + "B"
        in (TEN.pow(12)..TEN.pow(15) - ONE) -> f.format(this.divide(TEN.pow(12), DECIMAL32)) + "T"
        in (TEN.pow(15)..TEN.pow(18) - ONE) -> f.format(this.divide(TEN.pow(15), DECIMAL32)) + "q"
        in (TEN.pow(18)..TEN.pow(21) - ONE) -> f.format(this.divide(TEN.pow(18), DECIMAL32)) + "Q"
        in (TEN.pow(21)..TEN.pow(24) - ONE) -> f.format(this.divide(TEN.pow(21), DECIMAL32)) + "s"
        in (TEN.pow(24)..TEN.pow(27) - ONE) -> f.format(this.divide(TEN.pow(24), DECIMAL32)) + "S"
        in (TEN.pow(27)..TEN.pow(30) - ONE) -> f.format(this.divide(TEN.pow(27), DECIMAL32)) + "O"
        in (TEN.pow(30)..TEN.pow(33) - ONE) -> f.format(this.divide(TEN.pow(30), DECIMAL32)) + "N"
        in (TEN.pow(33)..TEN.pow(36) - ONE) -> f.format(this.divide(TEN.pow(33), DECIMAL32)) + "D"
        in (TEN.pow(36)..TEN.pow(39) - ONE) -> f.format(this.divide(TEN.pow(36), DECIMAL32)) + "uD"
        in (TEN.pow(39)..TEN.pow(42) - ONE) -> f.format(this.divide(TEN.pow(39), DECIMAL32)) + "dD"
        in (TEN.pow(42)..TEN.pow(45) - ONE) -> f.format(this.divide(TEN.pow(42), DECIMAL32)) + "tD"
        in (TEN.pow(45)..TEN.pow(48) - ONE) -> f.format(this.divide(TEN.pow(45), DECIMAL32)) + "qD"
        in (TEN.pow(48)..TEN.pow(51) - ONE) -> f.format(this.divide(TEN.pow(48), DECIMAL32)) + "QD"
        in (TEN.pow(51)..TEN.pow(54) - ONE) -> f.format(this.divide(TEN.pow(51), DECIMAL32)) + "sD"
        in (TEN.pow(54)..TEN.pow(57) - ONE) -> f.format(this.divide(TEN.pow(54), DECIMAL32)) + "SD"
        in (TEN.pow(57)..TEN.pow(60) - ONE) -> f.format(this.divide(TEN.pow(57), DECIMAL32)) + "OD"
        in (TEN.pow(60)..TEN.pow(63) - ONE) -> f.format(this.divide(TEN.pow(60), DECIMAL32)) + "ND"
        in (TEN.pow(63)..TEN.pow(66) - ONE) -> f.format(this.divide(TEN.pow(63), DECIMAL32)) + "V"
        in (TEN.pow(66)..TEN.pow(69) - ONE) -> f.format(this.divide(TEN.pow(66), DECIMAL32)) + "uV"
        in (TEN.pow(69)..TEN.pow(72) - ONE) -> f.format(this.divide(TEN.pow(69), DECIMAL32)) + "dV"
        in (TEN.pow(72)..TEN.pow(75) - ONE) -> f.format(this.divide(TEN.pow(72), DECIMAL32)) + "tV"
        in (TEN.pow(75)..TEN.pow(78) - ONE) -> f.format(this.divide(TEN.pow(75), DECIMAL32)) + "qV"
        in (TEN.pow(78)..TEN.pow(81) - ONE) -> f.format(this.divide(TEN.pow(78), DECIMAL32)) + "QV"
        in (TEN.pow(81)..TEN.pow(84) - ONE) -> f.format(this.divide(TEN.pow(81), DECIMAL32)) + "sV"
        in (TEN.pow(84)..TEN.pow(87) - ONE) -> f.format(this.divide(TEN.pow(84), DECIMAL32)) + "SV"
        in (TEN.pow(87)..TEN.pow(90) - ONE) -> f.format(this.divide(TEN.pow(87), DECIMAL32)) + "OV"
        in (TEN.pow(90)..TEN.pow(93) - ONE) -> f.format(this.divide(TEN.pow(90), DECIMAL32)) + "NV"
        in (TEN.pow(93)..TEN.pow(96) - ONE) -> f.format(this.divide(TEN.pow(93), DECIMAL32)) + "Tg"
        in (TEN.pow(96)..TEN.pow(99) - ONE) -> f.format(this.divide(TEN.pow(96), DECIMAL32)) + "uTG"
        in (TEN.pow(99)..TEN.pow(102) - ONE) -> f.format(this.divide(TEN.pow(99), DECIMAL32)) + "dTg"
        in (TEN.pow(102)..TEN.pow(105) - ONE) -> f.format(this.divide(TEN.pow(102), DECIMAL32)) + "tTg"
        in (TEN.pow(105)..TEN.pow(108) - ONE) -> f.format(this.divide(TEN.pow(105), DECIMAL32)) + "qTg"
        in (TEN.pow(108)..TEN.pow(111) - ONE) -> f.format(this.divide(TEN.pow(108), DECIMAL32)) + "QTg"
        in (TEN.pow(111)..TEN.pow(114) - ONE) -> f.format(this.divide(TEN.pow(111), DECIMAL32)) + "sTg"
        in (TEN.pow(114)..TEN.pow(117) - ONE) -> f.format(this.divide(TEN.pow(114), DECIMAL32)) + "STg"
        in (TEN.pow(117)..TEN.pow(120) - ONE) -> f.format(this.divide(TEN.pow(117), DECIMAL32)) + "OTg"
        in (TEN.pow(120)..TEN.pow(123) - ONE) -> f.format(this.divide(TEN.pow(120), DECIMAL32)) + "NTg"
        else -> f.format(this)
    }
}

fun paddingCharacters(current: Any, longest: Any, character: String = " "): String {
    val currentLength = current.toString().length
    val longestLength = longest.toString().length

    if (longestLength < currentLength) throw IllegalArgumentException("Longest's length ($longestLength) must be longer than current's length ($currentLength)")

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

val CommandEvent.arguments: List<String> get() = if (args.isBlank()) emptyList() else args.split(Regex("""\s"""))

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

fun Iterable<BigDecimal>.product(): BigDecimal = reduce { acc, bonus -> acc * bonus }
fun Double.round(places: Int = 0) = BigDecimal(this).setScale(places, HALF_UP).toDouble()
fun List<BigDecimal>.sum(): BigDecimal = this.reduce { acc, duration -> acc + duration }
fun List<Duration>.sum(): Duration = this.reduce { acc, duration -> acc + duration }
operator fun Int.times(other: BigDecimal): BigDecimal = this.toBigDecimal() * other
operator fun BigDecimal.times(other: Int): BigDecimal = this.multiply(other.toBigDecimal())
operator fun BigDecimal.times(other: Long): BigDecimal = this.multiply(other.toBigDecimal())
operator fun BigDecimal.times(other: Duration): BigDecimal = this.multiply(other.standardSeconds.toBigDecimal())

// Taken from https://stackoverflow.com/a/13831245/3021748
fun sqrt(value: BigDecimal, scale: Int = 32): BigDecimal {
    var sqrt = BigDecimal(1)
    sqrt.setScale(scale + 3, FLOOR)
    var store = value
    var first = true
    do {
        if (!first) store = sqrt else first = false
        store.setScale(scale + 3, FLOOR)
        sqrt = value
            .divide(store, scale + 3, FLOOR)
            .add(store)
            .divide(BigDecimal(2), scale + 3, FLOOR)
    } while (store != sqrt)
    return sqrt.setScale(scale, FLOOR)
}

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


// AuxBrain

val EggInc.Game.soulBonus: Int get() = epicResearchList.find { it.id == "soul_eggs" }!!.level
val EggInc.Game.prophecyBonus: Int get() = epicResearchList.find { it.id == "prophecy_bonus" }!!.level
val EggInc.Simulation.habPopulation: List<BigDecimal> get() = habPopulationList.map { it.toBigDecimal() }
val EggInc.Game.bonusPerSoulEgg: BigDecimal
    get() {
        val soulEggBonus = BigDecimal(10 + soulBonus)
        val prophecyEggBonus = BigDecimal(1.05) + BigDecimal(0.01) * BigDecimal(prophecyBonus)
        return prophecyEggBonus.pow(prophecyEggs.toInt()) * soulEggBonus
    }
val EggInc.Game.earningsBonus: BigDecimal get() = BigDecimal(soulEggs) * bonusPerSoulEgg
val EggInc.Contract.finalGoal: BigDecimal get() = BigDecimal(goalsList.maxBy { it.targetAmount }!!.targetAmount)
val EggInc.LocalContract.finalGoal: BigDecimal get() = contract.finalGoal
val EggInc.CoopStatusResponse.eggsLaid: BigDecimal get() = BigDecimal(totalAmount)


// Messages

val Command.missingArguments get() = "Missing argument(s). Use `${commandClient.textualPrefix}${this.name} ${this.arguments}` without the brackets."
val Command.tooManyArguments get() = "Too many arguments. Use `${commandClient.textualPrefix}${this.name} ${this.arguments}` without the brackets."

// Joda Time

// TODO: Update when available as language feature
// val Duration.Companion.INFINITE get() = Duration(Long.MAX_VALUE)
val Duration.INFINITE get() = Duration(Long.MAX_VALUE)

// Coroutines

suspend fun <T, R> Iterable<T>.asyncMap(transform: suspend (T) -> R): List<R> = coroutineScope {
    map { async { transform(it) } }.awaitAll()
}