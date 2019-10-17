package nl.pindab0ter.eggbot

import com.auxbrain.ei.EggInc
import com.github.kittinunf.fuel.core.Body
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.ChannelType.*
import net.dv8tion.jda.core.entities.User
import nl.pindab0ter.eggbot.commands.Register
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.jda.commandClient
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.Period
import org.joda.time.PeriodType
import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder
import java.math.BigDecimal.*
import java.math.MathContext.DECIMAL128
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.*
import kotlin.math.roundToLong
import java.math.BigDecimal as BD


// Formatting

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
fun BD.formatInteger(): String = integerFormat.format(this)

fun BD.formatIllions(rounded: Boolean = false): String {
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
        else -> f.format(this)
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

inline fun <T> Iterable<T>.sumBy(selector: (T) -> BD): BD {
    var sum: BD = ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

operator fun Duration.div(other: Duration): Double? = try {
    this.millis.toDouble() / other.millis.toDouble()
} catch (e: Exception) {
    null
}

fun Iterable<BD>.product(): BD = reduce { acc, bonus -> acc * bonus }
fun List<BD>.sum(): BD = this.reduce { acc, duration -> acc + duration }
fun List<Duration>.sum(): Duration = this.reduce { acc, duration -> acc + duration }
operator fun Int.times(other: BD): BD = this.toBigDecimal() * other
operator fun BD.times(other: Int): BD = this.multiply(other.toBigDecimal())
operator fun BD.times(other: Long): BD = this.multiply(other.toBigDecimal())
operator fun BD.times(other: Duration): BD = this.multiply(other.standardSeconds.toBigDecimal())
operator fun BD.div(other: BD): BD = this.divide(other, DECIMAL128)


// Exceptions

class PropertyNotFoundException(override val message: String?) : Exception(message)


// JDA Utilities

fun CommandEvent.replyInDms(messages: List<String>) {
    var successful: Boolean? = null
    messages.forEachIndexed { i, message ->
        replyInDm(message, {
            successful = (successful ?: true) && true
            if (i == messages.size - 1 && isFromType(TEXT)) reactSuccess()
        }, {
            if (successful == null) replyWarning("Help cannot be sent because you are blocking Direct Messages.")
            successful = false
        })
    }
}

fun Command.hasPermission(
    author: User, role: String?): Boolean = role != null && author.mutualGuilds.any { guild ->
    guild.getMember(author).let { author ->
        author.isOwner || author.user.id == Config.ownerId || author.roles.any { memberRole ->
            guild.getRolesByName(role, true).any { guildRole ->
                memberRole.position >= guildRole.position
            }
        }
    }

// AuxBrain

val EggInc.Backup.Game.soulBonus: Int get() = epicResearchList.find { it.id == "soul_eggs" }!!.level
val EggInc.Backup.Game.prophecyBonus: Int get() = epicResearchList.find { it.id == "prophecy_bonus" }!!.level
val EggInc.Backup.Simulation.habPopulation: List<BD> get() = habPopulationList.map { it.toBigDecimal() }
val EggInc.Contract.finalGoal: BD get() = BD(goalsList.maxBy { it.targetAmount }!!.targetAmount)
val EggInc.LocalContract.finalGoal: BD get() = contract.finalGoal
val EggInc.LocalContract.finished: Boolean get() = BD(lastAmountWhenRewardGiven) > contract.finalGoal
val EggInc.CoopStatusResponse.eggsLaid: BD get() = BD(totalAmount)
fun List<EggInc.Backup>.findContract(contractId: String): EggInc.LocalContract? = filter { backup ->
    backup.contracts.contractsList.plus(backup.contracts.archiveList).any { contract ->
        contract.contract.id == contractId
    }
}.maxBy { backup -> backup.approxTime }?.let { backup ->
    backup.contracts.contractsList.plus(backup.contracts.archiveList).find { contract ->
        contract.contract.id == contractId
    }
}

// @formatter:off
val EggInc.HabLevel.capacity: BD get() = when(this) {
    EggInc.HabLevel.NO_HAB ->                      ZERO
    EggInc.HabLevel.COOP ->                     BD(250)
    EggInc.HabLevel.SHACK ->                    BD(500)
    EggInc.HabLevel.SUPER_SHACK ->            BD(1_000)
    EggInc.HabLevel.SHORT_HOUSE ->            BD(2_000)
    EggInc.HabLevel.THE_STANDARD ->           BD(5_000)
    EggInc.HabLevel.LONG_HOUSE ->            BD(10_000)
    EggInc.HabLevel.DOUBLE_DECKER ->         BD(20_000)
    EggInc.HabLevel.WAREHOUSE ->             BD(50_000)
    EggInc.HabLevel.CENTER ->               BD(100_000)
    EggInc.HabLevel.BUNKER ->               BD(200_000)
    EggInc.HabLevel.EGGKEA ->               BD(500_000)
    EggInc.HabLevel.HAB_1000 ->           BD(1_000_000)
    EggInc.HabLevel.HANGAR ->             BD(2_000_000)
    EggInc.HabLevel.TOWER ->              BD(5_000_000)
    EggInc.HabLevel.HAB_10_000 ->        BD(10_000_000)
    EggInc.HabLevel.EGGTOPIA ->          BD(25_000_000)
    EggInc.HabLevel.MONOLITH ->          BD(50_000_000)
    EggInc.HabLevel.PLANET_PORTAL ->    BD(100_000_000)
    EggInc.HabLevel.CHICKEN_UNIVERSE -> BD(600_000_000)
    EggInc.HabLevel.UNRECOGNIZED ->                ZERO
}

val EggInc.VehicleType.capacity: BD get() = when (this) {
    EggInc.VehicleType.UNRECOGNIZED ->                  ZERO
    EggInc.VehicleType.TRIKE ->                    BD(5_000)
    EggInc.VehicleType.TRANSIT ->                 BD(15_000)
    EggInc.VehicleType.PICKUP ->                  BD(50_000)
    EggInc.VehicleType.VEHICLE_10_FOOT ->        BD(100_000)
    EggInc.VehicleType.VEHICLE_24_FOOT ->        BD(250_000)
    EggInc.VehicleType.SEMI ->                   BD(500_000)
    EggInc.VehicleType.DOUBLE_SEMI ->          BD(1_000_000)
    EggInc.VehicleType.FUTURE_SEMI ->          BD(5_000_000)
    EggInc.VehicleType.MEGA_SEMI ->           BD(15_000_000)
    EggInc.VehicleType.HOVER_SEMI ->          BD(30_000_000)
    EggInc.VehicleType.QUANTUM_TRANSPORTER -> BD(50_000_000)
    EggInc.VehicleType.HYPERLOOP_TRAIN ->     BD(50_000_000)
}

// @formatter:on


// Messages

val Command.missingArguments get() = "Missing argument(s). Use `${commandClient.textualPrefix}${this.name} ${this.arguments}` without the brackets."
val Command.tooManyArguments get() = "Too many arguments. Use `${commandClient.textualPrefix}${this.name} ${this.arguments}` without the brackets."


// Coroutines

suspend fun <T, R> Iterable<T>.asyncMap(transform: suspend (T) -> R): List<R> = coroutineScope {
    map { async { transform(it) } }.awaitAll()
}