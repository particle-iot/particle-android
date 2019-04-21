package io.particle.mesh.ui

import android.view.View
import androidx.annotation.IdRes
import androidx.navigation.findNavController


internal fun View.navigateOnClick(@IdRes actionId: Int) {
    this.setOnClickListener { findNavController().navigate(actionId) }
}
