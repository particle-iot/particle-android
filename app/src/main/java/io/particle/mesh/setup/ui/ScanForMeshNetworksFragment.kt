package io.particle.mesh.setup.ui


import android.os.Bundle
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.particle.firmwareprotos.ctrl.mesh.Mesh.NetworkInfo
import io.particle.mesh.common.Result
import io.particle.mesh.setup.ui.utils.easyDiffUtilCallback
import io.particle.mesh.setup.ui.utils.inflateRow
import io.particle.mesh.setup.utils.safeToast
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_scan_for_mesh_networks.view.*
import kotlinx.android.synthetic.main.row_mesh_networks.view.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch


class ScanForMeshNetworksFragment : BaseMeshSetupFragment() {

    private lateinit var adapter: ScannedMeshNetworksAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        adapter = ScannedMeshNetworksAdapter { onMeshNetworkSelected(it.meshNetworkInfo) }
        val root = inflater.inflate(R.layout.fragment_scan_for_mesh_networks, container, false)
        root.recyclerView.adapter = adapter
        return root
    }

    override fun onResume() {
        super.onResume()
        launch { scan() }
    }

    private fun onMeshNetworkSelected(networkInfo: NetworkInfo) {
        setupController.updateOtherParams(setupController.otherParams.value!!.copy(
                networkInfo = networkInfo
        ))
        findNavController().navigate(
                R.id.action_scanForMeshNetworksFragment_to_manualCommissioningAddToNetworkFragment
        )
    }

    private suspend fun scan() {
        val result = setupController.targetDevice!!.sendScanNetworks()
        launch(UI) {
            when (result) {
                is Result.Error,
                is Result.Absent -> {
                    if (isResumed) {
                        requireActivity().safeToast("Unable to scan for networks")
                    }
                }
                is Result.Present -> {
                    val networks = result.value.networksList
                    if (!networks.isEmpty()) {
                        adapter.submitList(networks.map { ScannedMeshNetwork(it.name, it) })
                    }
                    if (isResumed) {
                        delay(1000)
                        launch { scan() }
                    }
                }
            }
        }
    }

}


private data class ScannedMeshNetwork(
        val name: String,
        val meshNetworkInfo: NetworkInfo
)


private class ScannedMeshNetworkHolder(var rowRoot: View) : RecyclerView.ViewHolder(rowRoot) {
    val rowLine1 = rowRoot.row_line_1
    val rowLine2 = rowRoot.row_line_2
}


private class ScannedMeshNetworksAdapter(
        private val onItemClicked: (ScannedMeshNetwork) -> Unit
) : ListAdapter<ScannedMeshNetwork, ScannedMeshNetworkHolder>(
        easyDiffUtilCallback { deviceData: ScannedMeshNetwork -> deviceData.meshNetworkInfo }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScannedMeshNetworkHolder {
        return ScannedMeshNetworkHolder(inflateRow(parent, R.layout.row_select_device))
    }

    override fun onBindViewHolder(holder: ScannedMeshNetworkHolder, position: Int) {
        val item = getItem(position)

        holder.rowLine1.text = item.name
        holder.rowLine2.text = ""

        holder.rowRoot.setOnClickListener { onItemClicked(item) }
    }

}

