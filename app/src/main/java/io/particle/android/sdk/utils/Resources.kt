package io.particle.android.sdk.utils

import android.content.Context
import androidx.annotation.RawRes
import okio.buffer
import okio.source


fun Context.readRawResourceBytes(@RawRes resId: Int): ByteArray {
    val stream = this.resources.openRawResource(resId)
    return stream.source().buffer().use {
        it.readByteArray()
    }
}
}