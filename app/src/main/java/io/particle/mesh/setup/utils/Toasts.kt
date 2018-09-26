package io.particle.mesh.setup.utils

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch


fun Context.safeToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    launch(UI) { Toast.makeText(this@safeToast, text, duration).show() }
}
