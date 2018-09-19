package io.particle.mesh.common

/**
 * Some simple functional tools and aliases
 */


typealias Predicate<T> = (T) -> Boolean
typealias Procedure<T> = (T) -> Unit


fun <T> nonNull(): Predicate<T> = { x -> x != null }


inline infix fun <T> Predicate<T>.and(crossinline other: Predicate<T>): Predicate<T> {
    return { this(it) && other(it) }
}


inline infix fun <T> Predicate<T>.not(crossinline other: Predicate<T>): Predicate<T> {
    return negate(other)
}


inline fun <T> negate(crossinline toNegate: Predicate<T>): Predicate<T> {
    return { !toNegate(it) }
}

