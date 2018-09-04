package io.particle.mesh.setup.flow.modules.meshsetup

import android.arch.lifecycle.MutableLiveData
import android.support.annotation.WorkerThread
import io.particle.firmwareprotos.ctrl.mesh.Mesh
import io.particle.mesh.common.Result
import io.particle.mesh.common.android.livedata.setOnMainThread
import io.particle.mesh.setup.flow.FlowManager
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

class TargetDeviceMeshNetworksScanner(
        private val flowManager: FlowManager
) : MutableLiveData<List<Mesh.NetworkInfo>?>() {

    override fun onActive() {
        super.onActive()
        launch {
            scanWhileActive()
        }
    }

    fun forceSingleScan() {
        launch {
            doScan()
        }
    }

    private suspend fun scanWhileActive() {
        // FIXME: add real error handling
        while (hasActiveObservers()) {
            doScan()
            delay(1500)
        }
    }

    @WorkerThread
    private suspend fun doScan() {
        val xceiver = flowManager.targetDeviceTransceiverLD.value!!
        val networksReply = xceiver.sendScanNetworks()
        val networks = when(networksReply) {
            is Result.Present -> networksReply.value.networksList
            is Result.Error,
            is Result.Absent -> {
                listOf()
            }
        }
        setOnMainThread(networks)
    }
}