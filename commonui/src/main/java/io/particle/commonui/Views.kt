package io.particle.commonui

import android.content.Context
import android.util.DisplayMetrics
import kotlin.math.ceil
import kotlin.math.floor


fun dpToPx(dpValue: Float, displayMetrics: DisplayMetrics): Int {
    return (dpValue * displayMetrics.density).round()
}


fun dpToPx(dpValue: Float, context: Context): Int {
    return dpToPx(dpValue, context.resources.displayMetrics)
}


fun dpToPx(dpValue: Int, context: Context): Int {
    return dpToPx(dpValue.toFloat(), context)
}


private fun Float.round(): Int {
    val rounded = if (this < 0) ceil(this - 0.5f) else floor(this + 0.5f)
    return rounded.toInt()
}
