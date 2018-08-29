package io.particle.mesh.setup.flow

import android.arch.lifecycle.LiveData
import android.support.annotation.IdRes
import androidx.navigation.NavController
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType


class FlowManager(
        val targetDeviceType: ParticleDeviceType,
        private val navControllerRef: LiveData<NavController?>
) {

    private var flow: Flow = Flow(this)

    private val navController: NavController?
        get() = navControllerRef.value

    fun startFlow() {
        flow.startFlow()
    }

    fun clearState() {
        flow.clearState()
        // FIXME: finish implementing!
    }

    fun navigate(@IdRes idRes: Int) {
        navController?.navigate(idRes)
    }

}