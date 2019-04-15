package io.particle.android.sdk.controlpanel

import android.view.View
import androidx.annotation.IdRes
import androidx.navigation.findNavController


internal fun View.navigateOnClick(@IdRes actionId: Int) {
    this.setOnClickListener { findNavController().navigate(actionId) }
}
