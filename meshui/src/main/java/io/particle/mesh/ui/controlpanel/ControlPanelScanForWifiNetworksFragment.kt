package io.particle.mesh.ui.controlpanel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.particle.android.common.easyDiffUtilCallback
import io.particle.firmwareprotos.ctrl.wifi.WifiNew
import io.particle.firmwareprotos.ctrl.wifi.WifiNew.ScanNetworksReply
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.WiFiStrength
import io.particle.mesh.setup.flow.WifiScanData
import io.particle.mesh.ui.R
import io.particle.mesh.ui.databinding.FragmentCpScanForWifiNetworksBinding
import io.particle.mesh.ui.databinding.PControlpanelRowWifiScanBinding
import io.particle.mesh.ui.inflateRow
import mu.KotlinLogging


class ControlPanelScanForWiFiNetworksFragment : BaseControlPanelFragment() {

    private val log = KotlinLogging.logger {}

    private lateinit var adapter: ScannedWifiNetworksAdapter
    private lateinit var wifiScannerLD: LiveData<List<WifiScanData>?>

    private var _binding: FragmentCpScanForWifiNetworksBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCpScanForWifiNetworksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = ScannedWifiNetworksAdapter { onWifiNetworkSelected(it.network) }

        binding.pScanforwifiList.adapter = adapter

        wifiScannerLD = flowUiListener!!.wifi.getWifiScannerForTargetDevice()
        wifiScannerLD.observe(this, Observer { onNetworksUpdated(it) })
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
    private val rowBinding = PControlpanelRowWifiScanBinding.bind(rowRoot)
    val ssid = rowBinding.pScanforwifiSsid
    val securityIcon = rowBinding.pScanforwifiSecurityIcon
    val strengthIcon = rowBinding.pScanforwifiStrengthIcon
}


private class ScannedWifiNetworksAdapter(
    private val onItemClicked: (ScannedWifiNetwork) -> Unit
) : ListAdapter<ScannedWifiNetwork, ScannedWifiNetworkHolder>(
    easyDiffUtilCallback { rowEntry: ScannedWifiNetwork -> rowEntry.network.ssid }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScannedWifiNetworkHolder {
        return ScannedWifiNetworkHolder(parent.inflateRow(R.layout.p_controlpanel_row_wifi_scan))
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

