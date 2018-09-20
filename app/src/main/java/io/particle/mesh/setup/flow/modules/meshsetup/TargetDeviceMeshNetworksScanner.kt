package io.particle.mesh.setup.flow.modules.meshsetup

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.WorkerThread
import io.particle.firmwareprotos.ctrl.mesh.Mesh
import io.particle.mesh.common.Result
import io.particle.mesh.common.android.livedata.setOnMainThread
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.connection.ProtocolTransceiver
import io.particle.mesh.setup.flow.FlowManager
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

class TargetDeviceMeshNetworksScanner(
        private val targetXceiverLD: LiveData<ProtocolTransceiver?>
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
            delay(2000)
        }
    }

    @WorkerThread
    private suspend fun doScan() {
        val xceiver = targetXceiverLD.value!!
        val networksReply = xceiver.sendScanNetworks()
        val networks = when(networksReply) {
            is Result.Present -> networksReply.value.networksList
            is Result.Error,
            is Result.Absent -> {
                listOf()
            }
        }
        if (value.truthy() && networks.isEmpty()) {
            return  // don't replace results with non results (at least for MVP)
        }
        setOnMainThread(networks)
    }
}