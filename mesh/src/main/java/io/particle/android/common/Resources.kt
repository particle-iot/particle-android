package io.particle.android.common

import android.content.Context
import android.net.Uri
import androidx.annotation.RawRes


fun Context.buildRawResourceUri(@RawRes rawRes: Int): Uri {
    return Uri.parse("android.resource://${this.packageName}/$rawRes")
}