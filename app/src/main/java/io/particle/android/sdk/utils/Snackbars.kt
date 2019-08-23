package io.particle.android.sdk.utils

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.google.android.material.snackbar.BaseTransientBottomBar.Behavior
import com.google.android.material.snackbar.Snackbar
import io.particle.commonui.dpToPx


enum class SnackbarDuration(val intVal: Int) {
    LENGTH_SHORT(-1),
    LENGTH_LONG(0),
    LENGTH_INDEFINITE(-2)
}


fun View?.snackbar(
    message: CharSequence,
    duration: SnackbarDuration = SnackbarDuration.LENGTH_SHORT
) {
    if (this == null) {
        return
    }
    val snackbar = Snackbar.make(this, message, duration.intVal)
    // set elevation to ensure we actually display above all other views
    snackbar.view.elevation = dpToPx(12, this.context).toFloat()
    snackbar.show()
}


fun Activity?.snackbarInRootView(
    message: CharSequence,
    duration: SnackbarDuration = SnackbarDuration.LENGTH_SHORT
) {
    if (this == null) {
        return
    }
    val contentRoot = (this.findViewById(android.R.id.content) as ViewGroup)
    val myRoot = contentRoot.getChildAt(0)
    Snackbar.make(myRoot, message, duration.intVal).show()
}


fun Activity?.snackbarInRootView(
    @StringRes messageId: Int,
    duration: SnackbarDuration = SnackbarDuration.LENGTH_SHORT
) {
    if (this == null) {
        return
    }
    this.snackbarInRootView(getString(messageId), duration)
}
