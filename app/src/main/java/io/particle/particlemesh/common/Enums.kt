package io.particle.particlemesh.common

import android.util.SparseArray


/**
 * Build a [SparseArray] which maps to the given set of array values.
 *
 * Useful for writing functions that turn known Ints into a matching enum value.
 */
fun <T> buildIntValueMap(values: Array<T>, transformer: (T) -> Int): SparseArray<T> {
    val intValueMap = SparseArray<T>(values.size)
    for (v in values) {
        val key = transformer(v)
        intValueMap.put(key, v)
    }
    return intValueMap
}
