package io.particle.mesh.ui.utils

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment


fun Fragment.getResOrEmptyString(@StringRes resId: Int?): String {
    return if (resId == null) "" else getString(resId)
}