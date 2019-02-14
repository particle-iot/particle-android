package io.particle.mesh.common.android

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.particle.mesh.setup.utils.runOnMainThread


class SimpleLifecycleOwner : LifecycleOwner {

    private val registry = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle = registry

    fun setNewState(newState: Lifecycle.State) {
        runOnMainThread { registry.markState(newState) }
    }

}