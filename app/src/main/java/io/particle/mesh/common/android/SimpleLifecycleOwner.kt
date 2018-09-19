package io.particle.mesh.common.android

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LifecycleRegistry
import io.particle.mesh.setup.utils.runOnMainThread


class SimpleLifecycleOwner : LifecycleOwner {

    private val registry = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle = registry

    fun setNewState(newState: Lifecycle.State) {
        runOnMainThread { registry.markState(newState) }
    }

}