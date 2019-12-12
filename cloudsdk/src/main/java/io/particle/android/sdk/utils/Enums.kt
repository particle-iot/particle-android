package io.particle.android.sdk.utils


import androidx.collection.ArrayMap
import androidx.collection.SparseArrayCompat
import java.lang.Exception


// Errors for use with enum classes which use the functions below
class UnknownEnumIntValueException(intValue: Int) : Exception("Unknown enum value for $intValue")

class UnknownEnumStringValueException(stringValue: String) :
    Exception("Unknown enum value for $stringValue")


/**
 * Build a [SparseArray] which maps to the given set of array values.
 *
 * Useful for writing functions that turn known Ints into a matching enum value.
 */
fun <T> buildIntValueMap(values: Array<T>, transformer: (T) -> Int): SparseArrayCompat<T> {
    val intValueMap = SparseArrayCompat<T>(values.size)
    for (v in values) {
        val key = transformer(v)
        if (intValueMap.containsKey(key)) {
            throw IllegalArgumentException("Duplicate key value found: key=$key")
        }
        intValueMap.put(key, v)
    }
    return intValueMap
}


/**
 * Build an [ArrayMap] which maps to the given set of array values.
 *
 * Useful for writing functions that turn known strings into a matching enum value.
 */
fun <T> buildStringValueMap(values: Array<T>, transformer: (T) -> String): ArrayMap<String, T> {
    return ArrayMap<String, T>().apply {
        for (v in values) {
            val key = transformer(v)
            if (this.containsKey(key)) {
                throw IllegalArgumentException("Duplicate key value found: key=$key")
            }
            this[key] = v
        }
    }
}
