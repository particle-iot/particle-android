package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.phrase.Phrase
import io.particle.firmwareprotos.ctrl.mesh.Mesh.NetworkInfo
import io.particle.mesh.R
import io.particle.mesh.setup.flow.modules.device.NetworkSetupType
import io.particle.mesh.setup.ui.utils.easyDiffUtilCallback
import io.particle.mesh.setup.ui.utils.inflateRow
import kotlinx.android.synthetic.main.fragment_scan_for_mesh_networks.view.*
import kotlinx.android.synthetic.main.p_scanformeshnetwork_row_select_mesh_network.view.*


class ScanForMeshNetworksFragment : BaseMeshSetupFragment() {

    private lateinit var adapter: ScannedMeshNetworksAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fm = flowManagerVM.flowManager!!
        fm.meshSetupModule.targetDeviceVisibleMeshNetworksLD.observe(
            this,
            Observer { onNetworksUpdated(it) }
        )
        adapter = ScannedMeshNetworksAdapter { onMeshNetworkSelected(it.meshNetworkInfo) }
        val root = inflater.inflate(R.layout.fragment_scan_for_mesh_networks, container, false)
        root.recyclerView.adapter = adapter

        root.action_create_new_network.setOnClickListener {
            root.progressBar2.visibility = View.INVISIBLE
            fm.deviceModule.updateNetworkSetupType(NetworkSetupType.AS_GATEWAY)
            fm.meshSetupModule.onUserSelectedCreateNewNetwork()
        }

        root.action_create_new_network.visibility =
            if (fm.meshSetupModule.showNewNetworkOptionInScanner) View.VISIBLE else View.GONE
        return root
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
        flowManagerVM.flowManager!!.deviceModule.updateNetworkSetupType(NetworkSetupType.JOINER)
        flowManagerVM.flowManager!!.meshSetupModule.updateSelectedMeshNetworkToJoin(networkInfo)
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

