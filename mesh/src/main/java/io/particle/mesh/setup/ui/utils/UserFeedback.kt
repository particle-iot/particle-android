package io.particle.mesh.setup.ui.utils

import android.content.Context
import android.graphics.Typeface
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import io.particle.mesh.common.truthy
import io.particle.mesh.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


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

    GlobalScope.launch(Dispatchers.Main) {
        val tv: TextView? = view?.findViewById(progressStage)
        val ctx = tv?.context ?: return@launch
        val color = ContextCompat.getColor(ctx, R.color.p_text_color_primary)
        tv.setTextColor(color)
        tv.setTypeface(null, Typeface.BOLD)
    }
}
