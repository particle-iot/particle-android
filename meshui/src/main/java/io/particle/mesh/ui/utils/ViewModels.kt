package io.particle.mesh.ui.utils

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


inline fun <reified T : ViewModel> FragmentActivity.getViewModel(): T {
    return ViewModelProvider(this).get(T::class.java)
}


inline fun <reified T : ViewModel> Fragment.getViewModel(): T {
    val activity = requireActivity()
    return activity.getViewModel()
}
