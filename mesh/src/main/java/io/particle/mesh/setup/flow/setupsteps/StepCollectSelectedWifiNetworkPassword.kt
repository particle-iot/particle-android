package io.particle.mesh.setup.flow.setupsteps

import androidx.annotation.WorkerThread
import io.particle.firmwareprotos.ctrl.wifi.WifiNew.Security.NO_SECURITY
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.FlowUiDelegate


class StepCollectSelectedWifiNetworkPassword(
    private val flowUi: FlowUiDelegate
) : MeshSetupStep() {

    @WorkerThread
    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.wifi.targetWifiNetworkPasswordLD.value.truthy()
            || ctxs.wifi.targetWifiNetworkLD.value?.security == NO_SECURITY
        ) {
            return
        }

        ctxs.wifi.targetWifiNetworkPasswordLD.nonNull(scopes).runBlockOnUiThreadAndAwaitUpdate(scopes) {
            flowUi.showSetWifiPasswordUi()
        }

        flowUi.showGlobalProgressSpinner(true)
    }

}