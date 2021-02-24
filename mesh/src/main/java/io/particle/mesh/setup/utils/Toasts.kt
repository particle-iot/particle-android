package io.particle.mesh.setup.utils

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


enum class ToastDuration(val length: Int) {
    SHORT(Toast.LENGTH_SHORT),
    LONG(Toast.LENGTH_LONG)
}


enum class ToastGravity(val asGravityInt: Int) {
    TOP(Gravity.TOP),
    CENTER(Gravity.CENTER),
    BOTTOM(Gravity.BOTTOM)
}


fun Context?.safeToast(
    text: CharSequence?,
    duration: ToastDuration = ToastDuration.SHORT,
    gravity: ToastGravity = ToastGravity.BOTTOM
) {
    if (this == null) {
        return
    }
    if (text == null) {
        Log.w("safeToast", "No text specified!")
        return
    }
    GlobalScope.launch(Dispatchers.Main) {
        val toast = Toast.makeText(this@safeToast, text, duration.length)
        toast.setGravity(gravity.asGravityInt, 0, 0)
        toast.show()
    }
}
