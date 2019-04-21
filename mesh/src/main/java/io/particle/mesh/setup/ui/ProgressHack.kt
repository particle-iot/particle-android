package io.particle.mesh.setup.ui

import androidx.annotation.AnyThread


interface ProgressHack {
    @AnyThread
    fun showGlobalProgressSpinner(show: Boolean)
}
