package nl.pindab0ter.eggbot.helpers

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

fun <T> Iterable<T>.init(): Iterable<T> = take((count() - 1).coerceAtLeast(0))
fun <T> Iterable<T>.tail(): Iterable<T> = filterIndexed { index, _ -> index > 0 }
fun <T> Iterable<T>.replaceLast(block: (T) -> T) = init().plus(block(last()))
fun <T> Iterable<T>.replace(newValue: T, predicate: (T) -> Boolean): List<T> = map { element ->
    if (predicate(element)) newValue else element
}

inline fun <T, R, V> Iterable<T>.mapCartesianProducts(
    other: Iterable<R>,
    transform: (a: T, b: R) -> V,
): List<V> = flatMap { a: T -> other.map { b: R -> transform(a, b) } }

fun <T> Collection<T>.interleave(other: Collection<T>): Collection<T> =
    zip(other).flatMap(Pair<T, T>::toList) + if (size > other.size) drop(other.size) else other.drop(size)

suspend fun <T, R> Iterable<T>.mapAsync(
    coroutineContext: CoroutineContext = Dispatchers.Default,
    transform: suspend (T) -> R,
): List<R> = coroutineScope {
    map { async(coroutineContext) { transform(it) } }.awaitAll()
}

suspend fun <T> Iterable<T>.onEachAsync(
    coroutineContext: CoroutineContext = Dispatchers.Default,
    action: suspend (T) -> Unit,
): Iterable<T> = coroutineScope {
    onEach { launch(coroutineContext) { action(it) } }
}


suspend fun <T, K, V> Iterable<T>.associateAsync(
    coroutineContext: CoroutineContext = Dispatchers.Default,
    transform: suspend (T) -> Pair<K, V>,
): Map<K, V> = coroutineScope {
    map { async(coroutineContext) { transform(it) } }.awaitAll()
}.toMap()

suspend fun <T> Iterable<T>.forEachAsync(
    coroutineContext: CoroutineContext = Dispatchers.Default,
    action: suspend (T) -> Unit,
): Unit = coroutineScope {
    forEach { launch(coroutineContext) { action(it) } }
}

object Typography {
    /** The character &zwsp; – zero-width space */
    const val zwsp = '\u200B'

    /** The character &zwj; – zero-width joiner */
    const val zwj = '\u200D'
}

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
