package io.particle.mesh.ui.setup


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.particle.firmwareprotos.ctrl.mesh.Mesh.NetworkInfo
import io.particle.mesh.setup.flow.context.NetworkSetupType
import io.particle.android.common.easyDiffUtilCallback
import io.particle.android.common.inflateRow
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.R
import kotlinx.android.synthetic.main.fragment_scan_for_mesh_networks.*
import kotlinx.android.synthetic.main.p_scanformeshnetwork_row_select_mesh_network.view.*


class ScanForMeshNetworksFragment : BaseFlowFragment() {

    private lateinit var adapter: ScannedMeshNetworksAdapter
    private lateinit var meshNetworkScannerLD: LiveData<List<NetworkInfo>?>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scan_for_mesh_networks, container, false)
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        meshNetworkScannerLD = flowUiListener.mesh.getTargetDeviceVisibleMeshNetworksLD()
        meshNetworkScannerLD.observe(
            this,
            Observer { onNetworksUpdated(it) }
        )
        adapter = ScannedMeshNetworksAdapter { onMeshNetworkSelected(it.meshNetworkInfo) }
        recyclerView.adapter = adapter

        action_create_new_network.setOnClickListener {
            progressBar2.visibility = View.INVISIBLE
            flowUiListener.mesh.updateNetworkSetupType(NetworkSetupType.AS_GATEWAY)
            flowUiListener.mesh.onUserSelectedCreateNewNetwork()
        }

        action_create_new_network.isVisible = flowUiListener.mesh.showNewNetworkOptionInScanner
    }

    private fun onNetworksUpdated(networks: List<NetworkInfo>?) {
        adapter.submitList(
            networks?.asSequence()
                ?.map { ScannedMeshNetwork(it.name, it) }
                ?.sortedBy { it.name }
                ?.toList()
        )
    }

    private fun onMeshNetworkSelected(networkInfo: NetworkInfo) {
        flowUiListener?.mesh?.updateNetworkSetupType(NetworkSetupType.AS_GATEWAY)
        flowUiListener?.mesh?.updateNetworkSetupType(NetworkSetupType.NODE_JOINER)
        flowUiListener?.mesh?.updateSelectedMeshNetworkToJoin(networkInfo)
    }
}


private data class ScannedMeshNetwork(
    val name: String,
    val meshNetworkInfo: NetworkInfo
)


private class ScannedMeshNetworkHolder(var rowRoot: View) : RecyclerView.ViewHolder(rowRoot) {
    val rowLine1 = rowRoot.row_line_1
}


private class ScannedMeshNetworksAdapter(
    private val onItemClicked: (ScannedMeshNetwork) -> Unit
) : ListAdapter<ScannedMeshNetwork, ScannedMeshNetworkHolder>(
    easyDiffUtilCallback { deviceData: ScannedMeshNetwork -> deviceData.meshNetworkInfo }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScannedMeshNetworkHolder {
        return ScannedMeshNetworkHolder(
            inflateRow(
                parent,
                R.layout.p_scanformeshnetwork_row_select_mesh_network
            )
        )
    }

    override fun onBindViewHolder(holder: ScannedMeshNetworkHolder, position: Int) {
        val item = getItem(position)
        holder.rowLine1.text = item.name
        holder.rowRoot.setOnClickListener { onItemClicked(item) }
    }
}

