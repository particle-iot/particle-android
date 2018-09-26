package io.particle.android.sdk.utils

import android.content.Context
import androidx.annotation.RawRes
import okio.Okio


fun Context.readRawResourceBytes(@RawRes resId: Int): ByteArray {
    val stream = this.resources.openRawResource(resId)
    val buffer = Okio.buffer(Okio.source(stream))
    return buffer.use { it.readByteArray() }
}