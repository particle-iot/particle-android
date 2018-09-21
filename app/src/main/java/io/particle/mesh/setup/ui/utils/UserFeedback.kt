package io.particle.mesh.setup.ui.utils

import android.content.Context
import android.graphics.Typeface
import android.support.annotation.IdRes
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.widget.TextView
import io.particle.mesh.common.truthy
import io.particle.sdk.app.R.color
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch


fun Context.quickDialog(text: String, optionalAction: (() -> Unit)? = null) {
    AlertDialog.Builder(this)
            .setPositiveButton(android.R.string.ok) { _, _ -> optionalAction?.invoke() }
            .setMessage(text)
            .create()
            .show()
}


internal fun Fragment.markProgress(update: Boolean?, @IdRes progressStage: Int) {
    if (!update.truthy()) {
        return
    }

    launch(UI) {
        val tv: TextView? = view?.findViewById(progressStage)
        val ctx = tv?.context ?: return@launch
        val color = ContextCompat.getColor(ctx, color.p_text_color_primary)
        tv.setTextColor(color)
        tv.setTypeface(null, Typeface.BOLD)
    }
}
