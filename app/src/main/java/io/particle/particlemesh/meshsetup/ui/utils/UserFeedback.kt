package io.particle.particlemesh.meshsetup.ui.utils

import android.content.Context
import android.support.v7.app.AlertDialog


fun Context.quickDialog(text: String, optionalAction: (() -> Unit)? = null) {
    AlertDialog.Builder(this)
            .setPositiveButton(android.R.string.ok) { _, _ -> optionalAction?.invoke() }
            .setMessage(text)
            .create()
            .show()
}