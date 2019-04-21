package io.particle.mesh.ui.setup


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.particle.firmwareprotos.ctrl.wifi.WifiNew
import io.particle.firmwareprotos.ctrl.wifi.WifiNew.ScanNetworksReply
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.WiFiStrength
import io.particle.android.common.easyDiffUtilCallback
import io.particle.android.common.inflateRow
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.R
import kotlinx.android.synthetic.main.fragment_scan_for_wi_fi_networks.*
import kotlinx.android.synthetic.main.p_mesh_row_wifi_scan.view.*
import mu.KotlinLogging


class ScanForWiFiNetworksFragment : BaseFlowFragment() {

    private lateinit var adapter: ScannedWifiNetworksAdapter

    private val log = KotlinLogging.logger {}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scan_for_wi_fi_networks, container, false)
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        adapter = ScannedWifiNetworksAdapter { onWifiNetworkSelected(it.network) }
        p_scanforwifi_list.adapter = adapter
        flowUiListener.wifi.getWifiScannerForTargetDevice().observe(
            this,
            Observer { onNetworksUpdated(it) }
        )
    }

    private fun onNetworksUpdated(networks: List<ScanNetworksReply.Network>?) {
        adapter.submitList(networks?.asSequence()
            ?.filter { it.ssid.truthy() }
            ?.distinctBy { it.ssid }
            ?.map { ScannedWifiNetwork(it.ssid, it) }
            ?.sortedBy { it.ssid }
            ?.sortedByDescending { it.wiFiStrength.sortValue }
            ?.toList()
        )
    }

    private fun onWifiNetworkSelected(networkInfo: ScanNetworksReply.Network) {
        flowUiListener?.wifi?.setWifiNetworkToConfigure(networkInfo)
    }

}


private data class ScannedWifiNetwork(
    val ssid: String,
    val network: ScanNetworksReply.Network
) {
    val wiFiStrength: WiFiStrength = WiFiStrength.fromInt(network.rssi)
}


private class ScannedWifiNetworkHolder(var rowRoot: View) : RecyclerView.ViewHolder(rowRoot) {
    val ssid = rowRoot.p_scanforwifi_ssid
    val securityIcon = rowRoot.p_scanforwifi_security_icon
    val strengthIcon = rowRoot.p_scanforwifi_strength_icon
}


private class ScannedWifiNetworksAdapter(
    private val onItemClicked: (ScannedWifiNetwork) -> Unit
) : ListAdapter<ScannedWifiNetwork, ScannedWifiNetworkHolder>(
    easyDiffUtilCallback { rowEntry: ScannedWifiNetwork -> rowEntry.network.ssid }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScannedWifiNetworkHolder {
        return ScannedWifiNetworkHolder(
            inflateRow(
                parent,
                R.layout.p_mesh_row_wifi_scan
            )
        )
    }

    override fun onBindViewHolder(holder: ScannedWifiNetworkHolder, position: Int) {
        val item = getItem(position)

        holder.ssid.text = item.ssid
        holder.securityIcon.isVisible = item.network.security != WifiNew.Security.NO_SECURITY
        holder.strengthIcon.setImageResource(item.wiFiStrength.iconValue)

        holder.rowRoot.setOnClickListener { onItemClicked(item) }
    }

    private val WiFiStrength.iconValue: Int
        get() {
            return when (this) {
                WiFiStrength.STRONG -> R.drawable.p_mesh_ic_wifi_strength_high
                WiFiStrength.MEDIUM -> R.drawable.p_mesh_ic_wifi_strength_medium
                WiFiStrength.WEAK -> R.drawable.p_mesh_ic_wifi_strength_low
            }
        }
}

