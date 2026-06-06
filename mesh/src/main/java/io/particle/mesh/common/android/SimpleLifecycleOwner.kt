package io.particle.mesh.common.android

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.utils.runOnMainThread


class SimpleLifecycleOwner : LifecycleOwner {

    private val registry = LifecycleRegistry(this)
    private val scopes = Scopes()

    override val lifecycle: Lifecycle
        get() = registry

    fun setNewState(newState: Lifecycle.State) {
        scopes.onMain { registry.currentState = newState }
    }

}