package io.particle.particlemesh.meshsetup.ui


import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.bluetooth.le.ScanFilter.Builder
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.ParcelUuid
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.toast
import androidx.navigation.fragment.findNavController
import io.particle.particlemesh.common.android.livedata.distinct
import io.particle.particlemesh.common.truthy
import io.particle.particlemesh.meshsetup.BT_SETUP_SERVICE_ID
import io.particle.particlemesh.meshsetup.buildMeshDeviceScanner
import io.particle.particlemesh.meshsetup.utils.safeToast
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_ble_pairing_progress.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging


class BLEPairingProgressFragment : BaseMeshSetupFragment() {

    private lateinit var scannerLD: LiveData<List<ScanResult>?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scannerLD = buildScanner(this, setupController.deviceToBeSetUpParams.value!!.serialNumber!!)
        scannerLD.observe(
                this,
                Observer { onMatchingDeviceFound(it) }
        )
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

        launch(UI) {
            val targetDevice = setupController.connectToTargetDevice(targetAddress)
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

        delay(2000)

        findNavController().navigate(
                R.id.action_BLEPairingProgressFragment_to_scanForMeshNetworksFragment
        )
    }

}


private const val BT_NAME_ID_LENGTH = 6
private val log = KotlinLogging.logger {}
private fun buildScanner(fragment: Fragment, serialNumber: String): LiveData<List<ScanResult>?> {

//     FIXME: STOP THIS HACK
//    val lastSix = "6EVFRE".toLowerCase()
    val lastSix = serialNumber.substring(serialNumber.length - BT_NAME_ID_LENGTH).toLowerCase()
    fragment.requireActivity().safeToast(
            "Scanning for devices ending with '$lastSix'",
            duration = Toast.LENGTH_LONG)
    log.info {"Scanning for devices sending with '$lastSix'"}

    val scannerAndSwitch = buildMeshDeviceScanner(
            fragment.context!!.applicationContext,
            { sr -> sr.device.name != null && sr.device.name.toLowerCase().endsWith(lastSix) },
            Builder().setServiceUuid(ParcelUuid(BT_SETUP_SERVICE_ID)).build()
    )
    scannerAndSwitch.toggleSwitch.value = true
    return scannerAndSwitch.scannerLD.distinct()
}


fun Context.quickDialog(text: String, optionalAction: (() -> Unit)? = null) {
    AlertDialog.Builder(this)
            .setPositiveButton(android.R.string.ok) { _, _ -> optionalAction?.invoke() }
            .setMessage(text)
            .create()
            .show()
}