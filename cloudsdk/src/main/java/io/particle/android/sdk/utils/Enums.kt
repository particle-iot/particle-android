package io.particle.android.sdk.utils

import android.util.SparseArray
import androidx.core.util.containsKey
import java.lang.Exception


class UnknownEnumIntValueException(intValue: Int) : Exception("Unknown enum value for $intValue")


/**
 * Build a [SparseArray] which maps to the given set of array values.
 *
 * Useful for writing functions that turn known Ints into a matching enum value.
 */
fun <T> buildIntValueMap(values: Array<T>, transformer: (T) -> Int): SparseArray<T> {
    val intValueMap = SparseArray<T>(values.size)
    for (v in values) {
        val key = transformer(v)
        if (intValueMap.containsKey(key)) {
            throw IllegalArgumentException("Duplicate key value found: key=$key")
        }
        intValueMap.put(key, v)
    }
    return intValueMap
}
