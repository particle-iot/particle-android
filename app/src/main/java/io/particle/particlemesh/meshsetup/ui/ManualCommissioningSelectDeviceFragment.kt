package io.particle.particlemesh.meshsetup.ui


import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.bluetooth.le.ScanFilter.Builder
import android.bluetooth.le.ScanResult
import android.os.Bundle
import android.os.ParcelUuid
import android.support.v4.app.Fragment
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.particle.particlemesh.bluetooth.connecting.BTDeviceAddress
import io.particle.particlemesh.common.Result
import io.particle.particlemesh.common.android.livedata.distinct
import io.particle.particlemesh.meshsetup.connection.BT_SETUP_SERVICE_ID
import io.particle.particlemesh.meshsetup.connection.RequestSender
import io.particle.particlemesh.meshsetup.connection.buildMeshDeviceScanner
import io.particle.particlemesh.meshsetup.ui.utils.easyDiffUtilCallback
import io.particle.particlemesh.meshsetup.ui.utils.inflateRow
import io.particle.particlemesh.meshsetup.utils.safeToast
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_manual_commissioning_select_device.view.*
import kotlinx.android.synthetic.main.row_select_device.view.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging


class ManualCommissioningSelectDeviceFragment : BaseMeshSetupFragment() {

    private lateinit var scannerLD: LiveData<List<ScanResult>?>
    private lateinit var adapter: OnMeshDevicesAdapter

    private val scannedDevices = mutableSetOf<OnMeshDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val targetDeviceName = setupController.deviceToBeSetUpParams.value!!.bluetoothDeviceName!!
        scannerLD = buildScanner(this, targetDeviceName)
        scannerLD.observe(this, Observer { onScannedMeshDevicesFound(it) })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_manual_commissioning_select_device, container, false)
        adapter = OnMeshDevicesAdapter(this::onOnMeshDeviceSelected)
        root.recyclerView2.adapter = adapter
        return root
    }

    private fun onScannedMeshDevicesFound(scanResults: List<ScanResult>?) {
        if (scanResults == null) {
            return
        }

        scannedDevices.addAll(scanResults.map { OnMeshDevice(it.device.name, it.device.address) })
        adapter.submitList(scannedDevices.toMutableList())
    }

    private fun onOnMeshDeviceSelected(selected: OnMeshDevice) {
        val ctx = requireActivity().applicationContext

        launch(UI) {
            val commissioner = setupController.connectToCommissioner(
                    selected.deviceAddress,
                    // FIXME: use real mobile secret for commissioner here
                    "LOLWUTNOPE12345"
            )
            if (commissioner == null) {
                ctx.safeToast("Unable to connect to device ${selected.name}")
            } else {
                ctx.safeToast("Connected to ${selected.name}")
                launch { onCommissionerConnected(commissioner) }
            }
        }
    }

    private suspend fun onCommissionerConnected(commissioner: RequestSender) {
        val networkInfoReply = commissioner.sendGetNetworkInfo()
        val targetNetwork = setupController.otherParams.value!!.networkInfo!!
        val commissionerNetwork = when(networkInfoReply){
            is Result.Error,
            is Result.Absent -> {
                mahtoast("Could not get network info from commissioner")
                return
            }
            is Result.Present -> networkInfoReply.value.network
        }
        if (targetNetwork.extPanId != commissionerNetwork.extPanId) {
            log.warn { "Selected device is not on mesh network ${targetNetwork.name}" }
            mahtoast("Selected device is not on mesh network ${targetNetwork.name}")
            return
        }






//        launch(UI) {
//            findNavController().navigate(
//                    R.id.action_manualCommissioningSelectDeviceFragment_to_enterNetworkPasswordFragment
//            )
//        }





    }

    private fun mahtoast(msg: String) {
        requireActivity().safeToast(msg)
    }
}


private val log = KotlinLogging.logger {}

private fun buildScanner(fragment: Fragment, targetDeviceName: String): LiveData<List<ScanResult>?> {

    val scannerAndSwitch = buildMeshDeviceScanner(
            fragment.context!!.applicationContext,
            { sr -> sr.device.name != null && sr.device.name != targetDeviceName },
            Builder().setServiceUuid(ParcelUuid(BT_SETUP_SERVICE_ID)).build()
    )
    scannerAndSwitch.toggleSwitch.value = true
    return scannerAndSwitch.scannerLD.distinct()
}


private data class OnMeshDevice(
        val name: String,
        val deviceAddress: BTDeviceAddress
)


private class OnMeshDeviceHolder(var rowRoot: View) : RecyclerView.ViewHolder(rowRoot) {
    val rowLine1 = rowRoot.row_line_1
    val rowLine2 = rowRoot.row_line_2
    val image = rowRoot.row_image

    init {
        image.visibility = View.GONE
        rowLine2.text = ""
    }
}


private class OnMeshDevicesAdapter(
        private val onItemClicked: (OnMeshDevice) -> Unit
) : ListAdapter<OnMeshDevice, OnMeshDeviceHolder>(
        easyDiffUtilCallback { deviceData: OnMeshDevice -> deviceData.deviceAddress }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnMeshDeviceHolder {
        return OnMeshDeviceHolder(inflateRow(parent, R.layout.row_select_device))
    }

    override fun onBindViewHolder(holder: OnMeshDeviceHolder, position: Int) {
        val item = getItem(position)
        holder.rowLine1.text = item.name
        holder.rowRoot.setOnClickListener { onItemClicked(item) }
    }

}
