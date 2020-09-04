package nl.pindab0ter.eggbot.helpers

import kotlin.streams.toList

fun <T> Iterable<T>.init(): Iterable<T> = take((count() - 1).coerceAtLeast(0))
fun <T> Iterable<T>.replaceLast(block: (T) -> T) = init().plus(block(last()))
fun <T> Iterable<T>.replace(newValue: T, predicate: (T) -> Boolean): Iterable<T> {
    return map { element ->
        if (predicate(element)) newValue else element
    }
}

inline fun <T, R, V> Iterable<T>.mapCartesianProducts(
    other: Iterable<R>,
    transform: (a: T, b: R) -> V,
): List<V> = flatMap { a: T -> other.map { b -> transform(a, b) } }

fun <T> Collection<T>.interleave(other: Collection<T>): Collection<T> =
    zip(other).flatMap(Pair<T, T>::toList) + if (size > other.size) drop(other.size) else other.drop(size)

fun <T, R> List<T>.parallelMap(transform: (T) -> R): List<R> = parallelStream().map(transform).toList()

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
