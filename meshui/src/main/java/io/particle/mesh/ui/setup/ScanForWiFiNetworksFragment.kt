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
import io.particle.android.common.easyDiffUtilCallback
import io.particle.firmwareprotos.ctrl.wifi.WifiNew
import io.particle.firmwareprotos.ctrl.wifi.WifiNew.ScanNetworksReply
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.WiFiStrength
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.R
import io.particle.mesh.ui.databinding.FragmentScanForWiFiNetworksBinding
import io.particle.mesh.ui.databinding.PMeshRowWifiScanBinding
import io.particle.mesh.ui.inflateRow
import mu.KotlinLogging


class ScanForWiFiNetworksFragment : BaseFlowFragment() {

    private lateinit var adapter: ScannedWifiNetworksAdapter

    private val log = KotlinLogging.logger {}

    private var _binding: FragmentScanForWiFiNetworksBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentScanForWiFiNetworksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        adapter = ScannedWifiNetworksAdapter { onWifiNetworkSelected(it.network) }
        binding.pScanforwifiList.adapter = adapter
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
    private val binding = PMeshRowWifiScanBinding.bind(rowRoot)
    val ssid = binding.pScanforwifiSsid
    val securityIcon = binding.pScanforwifiSecurityIcon
    val strengthIcon = binding.pScanforwifiStrengthIcon
}


private class ScannedWifiNetworksAdapter(
    private val onItemClicked: (ScannedWifiNetwork) -> Unit
) : ListAdapter<ScannedWifiNetwork, ScannedWifiNetworkHolder>(
    easyDiffUtilCallback { rowEntry: ScannedWifiNetwork -> rowEntry.network.ssid }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScannedWifiNetworkHolder {
        return ScannedWifiNetworkHolder(parent.inflateRow(R.layout.p_mesh_row_wifi_scan))
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

