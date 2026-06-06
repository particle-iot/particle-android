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
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.R
import io.particle.mesh.ui.databinding.FragmentScanForMeshNetworksBinding
import io.particle.mesh.ui.databinding.PScanformeshnetworkRowSelectMeshNetworkBinding
import io.particle.mesh.ui.inflateRow


class ScanForMeshNetworksFragment : BaseFlowFragment() {

    private lateinit var adapter: ScannedMeshNetworksAdapter
    private lateinit var meshNetworkScannerLD: LiveData<List<NetworkInfo>?>

    private var _binding: FragmentScanForMeshNetworksBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentScanForMeshNetworksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        meshNetworkScannerLD = flowUiListener.mesh.getTargetDeviceVisibleMeshNetworksLD()
        meshNetworkScannerLD.observe(
            this,
            Observer { onNetworksUpdated(it) }
        )
        adapter = ScannedMeshNetworksAdapter { onMeshNetworkSelected(it.meshNetworkInfo) }
        binding.recyclerView.adapter = adapter

        binding.actionCreateNewNetwork.setOnClickListener {
            binding.progressBar2.visibility = View.INVISIBLE
            flowUiListener.mesh.updateNetworkSetupType(NetworkSetupType.AS_GATEWAY)
            flowUiListener.mesh.onUserSelectedCreateNewNetwork()
        }

        binding.actionCreateNewNetwork.isVisible = flowUiListener.mesh.showNewNetworkOptionInScanner
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
        flowUiListener?.mesh?.updateNetworkSetupType(NetworkSetupType.NODE_JOINER)
        flowUiListener?.mesh?.updateSelectedMeshNetworkToJoin(networkInfo)
    }
}


private data class ScannedMeshNetwork(
    val name: String,
    val meshNetworkInfo: NetworkInfo
)


private class ScannedMeshNetworkHolder(var rowRoot: View) : RecyclerView.ViewHolder(rowRoot) {
    val rowLine1 = PScanformeshnetworkRowSelectMeshNetworkBinding.bind(rowRoot).rowLine1
}


private class ScannedMeshNetworksAdapter(
    private val onItemClicked: (ScannedMeshNetwork) -> Unit
) : ListAdapter<ScannedMeshNetwork, ScannedMeshNetworkHolder>(
    easyDiffUtilCallback { deviceData: ScannedMeshNetwork -> deviceData.meshNetworkInfo }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScannedMeshNetworkHolder {
        return ScannedMeshNetworkHolder(
            parent.inflateRow(R.layout.p_scanformeshnetwork_row_select_mesh_network)
        )
    }

    override fun onBindViewHolder(holder: ScannedMeshNetworkHolder, position: Int) {
        val item = getItem(position)
        holder.rowLine1.text = item.name
        holder.rowRoot.setOnClickListener { onItemClicked(item) }
    }
}

