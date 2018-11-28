package io.particle.mesh.setup.utils

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


fun Context.safeToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    GlobalScope.launch(
        UI,
        CoroutineStart.DEFAULT,
        null,
        { Toast.makeText(this@safeToast, text, duration).show() })
}
