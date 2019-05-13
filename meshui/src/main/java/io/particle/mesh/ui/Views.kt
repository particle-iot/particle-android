package io.particle.mesh.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.core.graphics.drawable.DrawableCompat
import androidx.navigation.findNavController


fun View.navigateOnClick(@IdRes actionId: Int) {
    this.setOnClickListener { findNavController().navigate(actionId) }
}


fun View.setBackgroundTint(@ColorRes colorRes: Int) {
    val prevDrawable = this.background
    val drawable = prevDrawable.mutate()
    @Suppress("DEPRECATION")
    DrawableCompat.setTint(drawable, resources.getColor(colorRes))
    this.background = drawable
}


fun ViewGroup.inflate(@LayoutRes layoutId: Int, attach: Boolean = true): View {
    return LayoutInflater.from(this.context).inflate(
        layoutId, this, attach
    )
}


fun ViewGroup.inflateRow(@LayoutRes layoutId: Int): View {
    return this.inflate(layoutId, attach = false)
}


fun ViewGroup.inflateFragment(@LayoutRes layoutId: Int): View {
    return this.inflate(layoutId, attach = false)
}
