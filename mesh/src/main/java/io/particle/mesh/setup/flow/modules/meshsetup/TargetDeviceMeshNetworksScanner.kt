package io.particle.mesh.setup.flow.modules.meshsetup

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.particle.firmwareprotos.ctrl.mesh.Mesh
import io.particle.mesh.common.Result
import io.particle.mesh.common.android.livedata.setOnMainThread
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.connection.ProtocolTransceiver
import io.particle.mesh.setup.flow.Scopes
import kotlinx.coroutines.delay
import mu.KotlinLogging


class TargetDeviceMeshNetworksScanner(
    private val targetXceiverLD: LiveData<ProtocolTransceiver?>,
    private val scopes: Scopes
) : MutableLiveData<List<Mesh.NetworkInfo>?>() {

    private val log = KotlinLogging.logger {}

    override fun onActive() {
        super.onActive()
        log.debug { "onActive()" }
        scopes.onWorker {
            scanWhileActive()
        }
    }

    fun forceSingleScan() {
        log.debug { "forceSingleScan()" }
        scopes.onWorker {
            doScan()
        }
    }

    private suspend fun scanWhileActive() {
        // FIXME: add real error handling
        while (hasActiveObservers()) {
            doScan()
            delay(3000)
        }
    }

    @WorkerThread
    private suspend fun doScan() {
        log.debug { "doScan()" }
        val xceiver = targetXceiverLD.value!!
        val networksReply = xceiver.sendScanNetworks()
        val networks = when (networksReply) {
            is Result.Present -> networksReply.value.networksList
            is Result.Error,
            is Result.Absent -> {
                listOf()
            }
        }
        if (value.truthy() && networks.isEmpty()) {
            return  // don't replace results with non results (at least for MVP)
        }

        val sortedUniqueNetworks = networks.map { it.extPanId to it }
            .toMap()
            .values
            .sortedBy { it.name }

        setOnMainThread(sortedUniqueNetworks)
    }
}