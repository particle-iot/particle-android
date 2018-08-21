package io.particle.particlemesh.meshsetup.ui


import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.bluetooth.le.ScanResult
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.particle.particlemesh.common.truthy
import io.particle.particlemesh.meshsetup.ui.utils.buildMatchingDeviceScanner
import io.particle.particlemesh.meshsetup.ui.utils.quickDialog
import io.particle.particlemesh.meshsetup.utils.safeToast
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_ble_pairing_progress.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging


class BLEPairingProgressFragment : BaseMeshSetupFragment() {

    private lateinit var scannerLD: LiveData<List<ScanResult>?>

    private val log = KotlinLogging.logger {}


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serialNum = setupController.deviceToBeSetUpParams.value!!.barcodeData!!.serialNumber
        scannerLD = buildMatchingDeviceScanner(this, serialNum)
        scannerLD.observe(this, Observer { onMatchingDeviceFound(it) })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_ble_pairing_progress, container, false)
    }

    private fun onMatchingDeviceFound(results: List<ScanResult>?) {
        if (!results.truthy()) {
            log.error { "Results list was empty or null (but this should never happen!)" }
//            QATool.illegalState("Results list was empty (but this should never happen!)")
            return
        }

        scannerLD.removeObservers(this)

        val device = results!![0].device
        val targetAddress = device.address
        val ctx = requireActivity().applicationContext

        setupController.setBTDeviceName(device.name)

        val mobileSecret = setupController.deviceToBeSetUpParams.value!!.barcodeData!!.mobileSecret

        launch(UI) {
            val targetDevice = setupController.connectToTargetDevice(targetAddress, mobileSecret)

            if (targetDevice == null) {
                ctx.quickDialog("Unable to connect to device ${device.name}.")
            } else {
                ctx.safeToast("Connected to ${device.name}")
                onDeviceConnected()
            }
        }
    }

    private suspend fun onDeviceConnected() {
        progressBar.visibility = View.GONE
        state_success.visibility = View.VISIBLE

        val name = setupController.deviceToBeSetUpParams.value!!.bluetoothDeviceName
        status_text.text = "Successfully paired with device $name"

        requireActivity().safeToast("Connected device!")

        delay(2000)

        findNavController().navigate(
                R.id.action_BLEPairingProgressFragment_to_scanForMeshNetworksFragment
        )
    }

}
