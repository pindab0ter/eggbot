package nl.pindab0ter.eggbot.utilities

import com.github.kittinunf.fuel.core.Body
import com.github.kittinunf.fuel.util.encodeBase64
import com.martiansoftware.jsap.JSAPResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.dv8tion.jda.api.entities.User
import nl.pindab0ter.eggbot.EggBot.adminRole
import nl.pindab0ter.eggbot.EggBot.botOwner
import nl.pindab0ter.eggbot.EggBot.guild
import nl.pindab0ter.eggbot.database.DiscordUser
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.Duration
import java.util.*

val ONE_YEAR get() = Duration(DateTime.now(), DateTime.now().plusYears(1))

fun Body.decodeBase64(): ByteArray = Base64.getDecoder().decode(toByteArray())
fun ByteArray.encodeBase64ToString(): String = String(encodeBase64())

fun <T> Iterable<T>.init() = take((count() - 1).coerceAtLeast(0))
fun <T> Iterable<T>.replaceLast(block: (T) -> T) = init().plus(block(last()))

suspend fun <T, R> Iterable<T>.asyncMap(transform: suspend (T) -> R): List<R> = coroutineScope {
    map { async { transform(it) } }.awaitAll()
}

inline fun <T, R, V> Iterable<T>.mapCartesianProducts(
    other: Iterable<R>,
    transform: (a: T, b: R) -> V
): List<V> = flatMap { a: T -> other.map { b -> transform(a, b) } }

fun <T> Collection<T>.interleave(other: Collection<T>): Collection<T> =
    zip(other).flatMap(Pair<T, T>::toList) + if (size > other.size) drop(other.size) else other.drop(size)

val User.isRegistered: Boolean
    get() = transaction {
        DiscordUser.findById(id)?.farmers?.toList()?.sortedBy { it.inGameName }?.isNotEmpty() == true
    }

val User.isAdmin: Boolean
    get() = guild.getMember(this)?.let { author ->
        author.isOwner || author == botOwner || author.roles.contains(adminRole)
    } == true

fun JSAPResult.getIntOrNull(id: String) = if (contains(id)) getInt(id) else null
fun JSAPResult.getStringOrNull(id: String) = if (contains(id)) getString(id) else null

/**
 * Returns a string containing this char repeated [n] times.
 * @throws [IllegalArgumentException] when n < 0.
 */
fun Char.repeat(n: Int): String {
    require(n >= 0) { "Count 'n' must be non-negative, but was $n." }

    return when (n) {
        0 -> ""
        1 -> toString()
        else -> String(CharArray(n) { this })
    }
}
