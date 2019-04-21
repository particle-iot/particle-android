package io.particle.mesh.setup.flow.setupsteps

import androidx.annotation.WorkerThread
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.PricingImpactAction
import io.particle.android.sdk.cloud.PricingImpactNetworkType
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.setup.flow.FatalFlowException
import io.particle.mesh.setup.flow.Gen3ConnectivityType
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.NetworkSetupType
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.FlowUiDelegate
import io.particle.mesh.setup.flow.modules.meshsetup.MeshNetworkToJoin


class StepShowPricingImpact(
    private val flowUi: FlowUiDelegate,
    private val cloud: ParticleCloud
) : MeshSetupStep() {

    @WorkerThread
    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.pricingImpactConfirmedLD.value == true) {
            return
        }

        flowUi.showGlobalProgressSpinner(true)
        try {
            ensurePricingImpactRetrieved(ctxs)
        } finally {
            flowUi.showGlobalProgressSpinner(false)
        }

        ctxs.pricingImpactConfirmedLD.nonNull(scopes).runBlockOnUiThreadAndAwaitUpdate(scopes) {
            flowUi.showPricingImpactScreen()
        }
    }


    private fun ensurePricingImpactRetrieved(ctxs: SetupContexts) {
        val action = when (ctxs.device.networkSetupTypeLD.value) {
            NetworkSetupType.AS_GATEWAY -> PricingImpactAction.CREATE_NETWORK
            NetworkSetupType.STANDALONE -> PricingImpactAction.ADD_USER_DEVICE
            NetworkSetupType.NODE_JOINER -> throw FatalFlowException(
                "Should not be showing billing for joiners!"
            )
            null -> PricingImpactAction.ADD_NETWORK_DEVICE
        }

        val connType = ctxs.targetDevice.connectivityType
        val networkType = if (connType == Gen3ConnectivityType.CELLULAR) {
            PricingImpactNetworkType.CELLULAR
        } else {
            PricingImpactNetworkType.WIFI
        }

        val selectedNetwork = ctxs.mesh.meshNetworkToJoinLD.value
        val networkId = when (selectedNetwork) {
            is MeshNetworkToJoin.SelectedNetwork -> selectedNetwork.networkToJoin.networkId
            is MeshNetworkToJoin.CreateNewNetwork,
            null -> null
        }

        ctxs.cloud.pricingImpact = cloud.getPricingImpact(
            action = action,
            deviceId = ctxs.targetDevice.deviceId,
            networkId = networkId,
            networkType = networkType,
            iccid = ctxs.targetDevice.iccid
        )
    }
}