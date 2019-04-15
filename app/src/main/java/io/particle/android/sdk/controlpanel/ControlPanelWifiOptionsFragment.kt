package io.particle.android.sdk.controlpanel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.mesh.setup.SerialNumber
import io.particle.mesh.setup.ui.BarcodeData.CompleteBarcodeData
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_control_panel_wifi_options.*
import mu.KotlinLogging


class ControlPanelWifiOptionsFragment : BaseControlPanelFragment() {

    private val log = KotlinLogging.logger {}

    override val titleBarOptions = TitleBarOptions(
        R.string.p_common_wifi,
        showBackButton = true,
        showCloseButton = false
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_control_panel_wifi_options, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        p_controlpanel_wifi_join_new_network.setOnClickListener {
            meshModel.scopes.onMain { joinNewWifiClicked() }
        }
    }

    @MainThread
    private suspend fun joinNewWifiClicked() {
        meshModel.showGlobalProgressSpinner(true)
        val barcode = meshModel.scopes.withWorker { fetchBarcodeData(deviceId) }
        meshModel.showGlobalProgressSpinner(false)

        meshModel.flowRunner.startControlPanelWifiConfigFlow(deviceId, barcode)
    }

    @WorkerThread
    private fun fetchBarcodeData(deviceId: String): CompleteBarcodeData {
        val cloud = ParticleCloudSDK.getCloud()
        val device = cloud.getDevice(deviceId)

        val barcode = CompleteBarcodeData(
            serialNumber = SerialNumber(device.serialNumber!!),
            mobileSecret = device.mobileSecret!!
        )

        log.info { "built barcode: $barcode" }

        return barcode

    }
}
