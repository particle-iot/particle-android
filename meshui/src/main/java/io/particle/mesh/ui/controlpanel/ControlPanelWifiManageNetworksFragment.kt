package io.particle.mesh.ui.controlpanel


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import io.particle.android.common.easyDiffUtilCallback
import io.particle.firmwareprotos.ctrl.wifi.WifiNew.GetKnownNetworksReply
import io.particle.firmwareprotos.ctrl.wifi.WifiNew.Security.NO_SECURITY
import io.particle.firmwareprotos.ctrl.wifi.WifiNew.Security.UNRECOGNIZED
import io.particle.firmwareprotos.ctrl.wifi.WifiNew.Security.WEP
import io.particle.firmwareprotos.ctrl.wifi.WifiNew.Security.WPA2_PSK
import io.particle.firmwareprotos.ctrl.wifi.WifiNew.Security.WPA_PSK
import io.particle.firmwareprotos.ctrl.wifi.WifiNew.Security.WPA_WPA2_PSK
import io.particle.mesh.common.QATool
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.BarcodeData.CompleteBarcodeData
import io.particle.mesh.setup.connection.ProtocolTransceiver
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.throwOnErrorOrAbsent
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import io.particle.mesh.ui.inflateRow
import kotlinx.android.synthetic.main.fragment_control_panel_wifi_manage_networks.*
import kotlinx.android.synthetic.main.p_mesh_row_wifi_scan.view.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield


class ControlPanelWifiManageNetworksFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(
        R.string.p_control_panel_manage_wifi_title,
        showBackButton = true,
        showCloseButton = false
    )


    private lateinit var adapter: KnownWifiNetworksAdapter

    private var barcode: CompleteBarcodeData? = null
    private var transceiver: ProtocolTransceiver? = null
    private val scopes = Scopes()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_control_panel_wifi_manage_networks,
            container,
            false
        )
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        adapter = KnownWifiNetworksAdapter(::onWifiNetworkSelected)
        recyclerView.adapter = adapter

        scopes.onMain {
            startFlowWithBarcode { _, barcode ->
                this@ControlPanelWifiManageNetworksFragment.barcode = barcode
                reloadNetworks()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        barcode?.let { reloadNetworks() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scopes.cancelChildren()
    }

    @MainThread
    private fun reloadNetworks() {
        runWithTransceiver("Error reloading known networks") { xceiver, scopes ->
            val reply = scopes.withWorker {
                xceiver.sendGetKnownNetworksRequest().throwOnErrorOrAbsent()
            }
            onNetworksUpdated(reply.networksList)
        }
    }

    @MainThread
    private fun onNetworksUpdated(networks: List<GetKnownNetworksReply.Network>?) {
        val list = networks?.asSequence()
            ?.filter { it.ssid.truthy() }
            ?.distinctBy { it.ssid }
            ?.map { KnownWifiNetwork(it.ssid, it) }
            ?.sortedBy { it.ssid }
            ?.sortedByDescending { it.ssid }
            ?.toList()

        if (list?.isEmpty() == true) {
            p_cp_emptyview.isVisible = true
            recyclerView.isVisible = false
            adapter.submitList(emptyList())
        } else {
            p_cp_emptyview.isVisible = false
            recyclerView.isVisible = true
            adapter.submitList(list)
        }
    }

    private fun onWifiNetworkSelected(network: KnownWifiNetwork) {
        MaterialDialog.Builder(this.requireContext())
            .title("Remove Wi-Fi credentials?")
            .content("Are you sure you want to remove Wi-Fi credentials for '${network.ssid}'?")
            .positiveText("Remove")
            .negativeText(android.R.string.cancel)
            .onPositive { _, _ -> removeWifiNetwork(network) }
            .show()
    }

    private fun removeWifiNetwork(toRemove: KnownWifiNetwork) {
        flowSystemInterface.showGlobalProgressSpinner(true)
        runWithTransceiver("Error removing network ${toRemove.ssid}") { xceiver, scopes ->
            scopes.onWorker {
                xceiver.sendStartListeningMode()
                delay(100)
                xceiver.sendRemoveKnownNetworkRequest(toRemove.ssid)
                delay(1000)
                xceiver.sendStopListeningMode()
                scopes.onMain { reloadNetworks() }
            }
        }
    }

    private fun runWithTransceiver(
        errorMsg: String,
        toRun: suspend (ProtocolTransceiver, Scopes) -> Unit
    ) {
        flowSystemInterface.showGlobalProgressSpinner(true)
        var connected = false
        scopes.onMain {
            val xceiver = transceiver
            try {
                if (xceiver == null || !xceiver.isConnected) {
                    transceiver = flowRunner.getProtocolTransceiver(barcode!!, scopes)
                }
                connected = transceiver != null
                flowSystemInterface.showGlobalProgressSpinner(true)
                toRun(transceiver!!, scopes)

            } catch (ex: Exception) {
                QATool.log(ex.toString())
                val error = if (connected) errorMsg else "Error connecting to device"
                flowSystemInterface.dialogHack.newSnackbarRequest(error)
                if (isAdded) {
                    findNavController().popBackStack()
                }
            } finally {
                flowSystemInterface.showGlobalProgressSpinner(false)
            }
        }
    }

}


private data class KnownWifiNetwork(
    val ssid: String,
    val network: GetKnownNetworksReply.Network?
)


private class KnownWifiNetworkHolder(var rowRoot: View) : RecyclerView.ViewHolder(rowRoot) {
    val ssid = rowRoot.p_scanforwifi_ssid
    val securityIcon = rowRoot.p_scanforwifi_security_icon
    val strengthIcon = rowRoot.p_scanforwifi_strength_icon

    init {
        strengthIcon.isVisible = false
    }
}


private class KnownWifiNetworksAdapter(
    private val onItemClicked: (KnownWifiNetwork) -> Unit
) : ListAdapter<KnownWifiNetwork, KnownWifiNetworkHolder>(
    easyDiffUtilCallback { rowEntry: KnownWifiNetwork -> rowEntry.ssid }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KnownWifiNetworkHolder {
        return KnownWifiNetworkHolder(parent.inflateRow(R.layout.p_mesh_row_wifi_scan))
    }

    override fun onBindViewHolder(holder: KnownWifiNetworkHolder, position: Int) {
        val item = getItem(position)

        holder.ssid.text = item.ssid
        holder.securityIcon.isVisible = item.network?.isSecureNetwork ?: false

        holder.rowRoot.setOnClickListener { onItemClicked(item) }
    }
}


private val GetKnownNetworksReply.Network.isSecureNetwork: Boolean
    get() {
        return when (this.security) {
            WEP,
            WPA_PSK,
            WPA2_PSK,
            WPA_WPA2_PSK -> true
            NO_SECURITY,
            UNRECOGNIZED,
            null -> false
        }
    }