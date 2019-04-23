package io.particle.android.sdk.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes


fun ViewGroup.inflate(@LayoutRes layoutId: Int, attach: Boolean = true): View {
    return LayoutInflater.from(this.context).inflate(
        layoutId, this, attach
    )
}


fun ViewGroup.inflateRow(@LayoutRes layoutId: Int): View {
    return this.inflate(layoutId, attach = false)
}