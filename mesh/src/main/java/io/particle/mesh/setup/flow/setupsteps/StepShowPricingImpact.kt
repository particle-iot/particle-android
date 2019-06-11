package io.particle.mesh.setup.flow.setupsteps

import androidx.annotation.WorkerThread
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.PricingImpactAction
import io.particle.android.sdk.cloud.PricingImpactNetworkType
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.flow.context.NetworkSetupType
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.meshsetup.MeshNetworkToJoin


class StepShowPricingImpact(
    private val flowUi: FlowUiDelegate,
    private val cloud: ParticleCloud
) : MeshSetupStep() {

    @WorkerThread
    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.cloud.pricingImpactConfirmedLD.value == true) {
            return
        }

        flowUi.showGlobalProgressSpinner(true)
        ensurePricingImpactRetrieved(ctxs)

        ctxs.cloud.pricingImpactConfirmedLD
            .nonNull(scopes)
            .runBlockOnUiThreadAndAwaitUpdate(scopes) {
                flowUi.showPricingImpactScreen()
            }
    }
    
    private fun ensurePricingImpactRetrieved(ctxs: SetupContexts) {
        val action = when (ctxs.device.networkSetupTypeLD.value) {
            NetworkSetupType.AS_GATEWAY -> PricingImpactAction.CREATE_NETWORK
            NetworkSetupType.STANDALONE -> PricingImpactAction.ADD_USER_DEVICE
            NetworkSetupType.NODE_JOINER -> throw InvalidOperationException(
                "Should not be showing billing screen for joiners!"
            )
            null -> PricingImpactAction.ADD_NETWORK_DEVICE
        }

        val connType = ctxs.targetDevice.connectivityType
        val networkType = if (connType == Gen3ConnectivityType.CELLULAR && !ctxs.hasEthernet!!) {
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

        try {
            ctxs.cloud.pricingImpact = cloud.getPricingImpact(
                action = action,
                deviceId = ctxs.targetDevice.deviceId,
                networkId = networkId,
                networkType = networkType,
                iccid = ctxs.targetDevice.iccid
            )
        } catch (ex: Exception) {
            throw UnableToGetPricingInformationException(ex)
        }
    }
}