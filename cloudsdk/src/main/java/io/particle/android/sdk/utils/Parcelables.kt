package io.particle.android.sdk.utils


import android.os.Bundle
import android.os.Parcel
import androidx.collection.ArrayMap
import java.io.Serializable
import java.util.*


fun Parcel.readBoolean(): Boolean {
    return this.readInt() != 0
}


fun Parcel.writeBoolean(value: Boolean) {
    this.writeInt(if (value) 1 else 0)
}


fun Parcel.readStringList(): List<String> {
    val sourceList = ArrayList<String>()
    this.readStringList(sourceList)
    return sourceList
}

inline fun <reified T : Serializable> Parcel.readSerializableMap(): Map<String, T> {
    val map = ArrayMap<String, T>()
    val bundle = this.readBundle(T::class.java.classLoader)
    for (key in bundle!!.keySet()) {
        val serializable = bundle.getSerializable(key) as T
        map[key] = serializable
    }
    return map
}

fun <T : Serializable> Parcel.writeSerializableMap(map: Map<String, T>) {
    val b = Bundle()
    for ((key, value) in map) {
        b.putSerializable(key, value)
    }
    this.writeBundle(b)
}
