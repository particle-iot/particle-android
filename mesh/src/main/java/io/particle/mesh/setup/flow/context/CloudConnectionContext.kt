package io.particle.mesh.setup.flow.context

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.particle.android.sdk.cloud.ParticleNetwork
import io.particle.mesh.common.android.livedata.castAndPost
import io.particle.mesh.common.logged
import io.particle.mesh.setup.flow.Clearable
import mu.KotlinLogging


class CloudConnectionContext : Clearable {

    private val log = KotlinLogging.logger {}

    val targetDeviceNameToAssignLD: LiveData<String?> = MutableLiveData()
    val isTargetDeviceNamedLD: LiveData<Boolean?> = MutableLiveData()
    val shouldConnectToDeviceCloudConfirmed: LiveData<Boolean?> = MutableLiveData()

    var apiNetworks: List<ParticleNetwork>? by log.logged()
    var claimCode: String? by log.logged()
    var shouldBeClaimed: Boolean? by log.logged()
    var connectedToCloudCongratsUiShown by log.logged(false)
    var paymentCardOnFile by log.logged(false)

    private var checkEthernetGatewayUiShown by log.logged(false)

    override fun clearState() {
        log.info { "clearState()" }

        claimCode = null
        checkEthernetGatewayUiShown = false
        connectedToCloudCongratsUiShown = false
        paymentCardOnFile = false
        shouldBeClaimed = null

        val setToNulls = listOf(
            targetDeviceNameToAssignLD,
            isTargetDeviceNamedLD,
            shouldConnectToDeviceCloudConfirmed,
        )
        for (ld in setToNulls) {
            ld.castAndPost(null)
        }
    }

    fun updateIsTargetDeviceNamed(named: Boolean) {
        log.info { "updateIsTargetDeviceNamed(): $named" }
        isTargetDeviceNamedLD.castAndPost(named)
    }

    fun updateTargetDeviceNameToAssign(name: String) {
        log.info { "updateTargetDeviceNameToAssign(): $name" }
        targetDeviceNameToAssignLD.castAndPost(name)
    }

    fun updateShouldConnectToDeviceCloudConfirmed(confirmed: Boolean) {
        log.info { "updateShouldConnectToDeviceCloudConfirmed(): $confirmed" }
        shouldConnectToDeviceCloudConfirmed.castAndPost(confirmed)
    }

}