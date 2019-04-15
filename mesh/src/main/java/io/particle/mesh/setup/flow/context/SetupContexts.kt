package io.particle.mesh.setup.flow.context

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.particle.mesh.common.android.livedata.castAndPost
import io.particle.mesh.common.logged
import io.particle.mesh.setup.connection.ProtocolTransceiver
import io.particle.mesh.setup.flow.Clearable
import io.particle.mesh.setup.flow.FlowIntent
import io.particle.mesh.setup.flow.FlowType
import io.particle.mesh.setup.flow.Scopes
import mu.KotlinLogging


class SetupContexts(
    var scopes: Scopes = Scopes(),
    val ble: BLEContext = BLEContext(),
    val cellular: CellularContext = CellularContext(),
    val cloud: CloudConnectionContext = CloudConnectionContext(),
    val device: DeviceContext = DeviceContext(),
    val mesh: MeshContext = MeshContext(),
    val wifi: WifiContext = WifiContext()
) : Clearable {

    private val log = KotlinLogging.logger {}

    var flowIntent: FlowIntent? by log.logged()
    var currentFlow: List<FlowType> by log.logged(emptyList())
    var hasEthernet: Boolean? by log.logged()
    var meshNetworkFlowAdded: Boolean by log.logged(false)

    var singleStepCongratsMessage by log.logged("")

    // FIXME: this should go.  See notes on StepDetermineFlowAfterPreflow
    val getReadyNextButtonClickedLD: LiveData<Boolean?> = MutableLiveData()
    val pricingImpactConfirmedLD: LiveData<Boolean?> = MutableLiveData()



    override fun clearState() {
        val clearables = listOf(
            ble,
            cellular,
            cloud,
            device,
            mesh,
            wifi
        )
        for (c in clearables) {
            c.clearState()
        }

        val liveDatas = listOf(
            getReadyNextButtonClickedLD,
            pricingImpactConfirmedLD
        )
        for (ld in liveDatas) {
            ld.castAndPost(null)
        }

        flowIntent = null
        singleStepCongratsMessage = ""
        currentFlow = emptyList()
        hasEthernet = null
        meshNetworkFlowAdded = false
        scopes.cancelAll()
        scopes = Scopes()
    }

    fun requireTargetXceiver(): ProtocolTransceiver {
        return ble.targetDevice.transceiverLD.value!!
    }

    fun requireCommissionerXceiver(): ProtocolTransceiver {
        return ble.commissioner.transceiverLD.value!!
    }

    fun updateGetReadyNextButtonClicked(clicked: Boolean) {
        log.info { "updateGetReadyNextButtonClicked()" }
        getReadyNextButtonClickedLD.castAndPost(clicked)
    }

    fun updatePricingImpactConfirmed(clicked: Boolean) {
        log.info { "updatePricingImpactConfirmed(): $clicked" }
        pricingImpactConfirmedLD.castAndPost(clicked)
    }

}