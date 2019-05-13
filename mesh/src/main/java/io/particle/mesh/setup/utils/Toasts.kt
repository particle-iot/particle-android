package io.particle.mesh.setup.utils

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


enum class ToastDuration(val length: Int) {
    SHORT(Toast.LENGTH_SHORT),
    LONG(Toast.LENGTH_LONG)
}

fun Context.safeToast(text: CharSequence, duration: ToastDuration = ToastDuration.SHORT) {
    GlobalScope.launch(Dispatchers.Main) {
        Toast.makeText(this@safeToast, text, duration.length).show()
    }
}
