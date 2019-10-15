package nl.pindab0ter.eggbot.utilities

import com.github.kittinunf.fuel.core.Body
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.*

fun Body.decodeBase64(): ByteArray = Base64.getDecoder().decode(toByteArray())

fun <T> Iterable<T>.init() = take((count() - 1).coerceAtLeast(0))
fun <T> Iterable<T>.tail() = drop(1)
fun <T> Iterable<T>.replaceLast(block: (T) -> T) = init().plus(block(last()))

suspend fun <T, R> Iterable<T>.asyncMap(transform: suspend (T) -> R): List<R> = coroutineScope {
    map { async { transform(it) } }.awaitAll()
}