package io.particle.mesh.setup.utils

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


fun Context.safeToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    GlobalScope.launch(Dispatchers.Main) { Toast.makeText(this@safeToast, text, duration).show() }
}


fun Fragment.safeToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    requireActivity().safeToast(text, duration)
}