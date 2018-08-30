package io.particle.mesh.setup.flow

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.IdRes
import androidx.navigation.NavController
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.mesh.setup.ui.BarcodeData


class FlowManager(
        val targetDeviceType: ParticleDeviceType,
        private val navControllerRef: LiveData<NavController?>
) {

    val targetDeviceBarcodeLD: LiveData<BarcodeData?> = MutableLiveData()

    private var flow: Flow = Flow(this)

    private val navController: NavController?
        get() = navControllerRef.value

    fun startFlow() {
        flow.runFlow()
    }

    fun clearState() {
        flow.clearState()
        // FIXME: finish implementing!
    }

    fun navigate(@IdRes idRes: Int) {
        navController?.navigate(idRes)
    }

    fun updateTargetDeviceBarcode(barcodeData: BarcodeData?) {
        (targetDeviceBarcodeLD as MutableLiveData).postValue(barcodeData)
    }
}