package io.particle.particlemesh.meshsetup.utils

import android.content.Context
import android.widget.Toast
import androidx.core.widget.toast
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch


inline fun Context.safeToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    launch(UI) { this@safeToast.toast(text, duration) }
}
