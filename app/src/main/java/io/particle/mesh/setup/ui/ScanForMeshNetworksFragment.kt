package io.particle.mesh.setup.ui


import androidx.lifecycle.Observer
import android.os.Bundle
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.squareup.phrase.Phrase
import io.particle.firmwareprotos.ctrl.mesh.Mesh.NetworkInfo
import io.particle.mesh.common.QATool
import io.particle.mesh.setup.ui.utils.easyDiffUtilCallback
import io.particle.mesh.setup.ui.utils.inflateRow
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_scan_for_mesh_networks.view.*
import kotlinx.android.synthetic.main.row_mesh_networks.view.*
import java.lang.NullPointerException


class ScanForMeshNetworksFragment : BaseMeshSetupFragment() {

    private lateinit var adapter: ScannedMeshNetworksAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val fm = flowManagerVM.flowManager!!
        fm.meshSetupModule.targetDeviceVisibleMeshNetworksLD.observe(
                this,
                Observer { onNetworksUpdated(it) }
        )
        adapter = ScannedMeshNetworksAdapter { onMeshNetworkSelected(it.meshNetworkInfo) }
        val root = inflater.inflate(R.layout.fragment_scan_for_mesh_networks, container, false)
        root.recyclerView.adapter = adapter

        root.action_create_new_network.setOnClickListener {
            fm.meshSetupModule.onUserSelectedCreateNewNetwork()
        }

        if (fm.meshSetupModule.showNewNetworkOptionInScanner) {
            root.setup_header_text.text = Phrase.from(view, R.string.p_scanfornetworks_gateway_flow_title)
                    .put("product_type", fm.getTypeName(root.context))
                    .format()
            root.progressBar2.visibility = View.INVISIBLE
            root.recyclerView.visibility = View.INVISIBLE
            root.action_create_new_network.visibility = View.VISIBLE
        } else {
            fm.cloudConnectionModule.currentDeviceName.observe(this, Observer {
                if (it == null) {
                    QATool.report(NullPointerException("Device name is null."))
                }

                root.setup_header_text.text = Phrase.from(view, R.string.p_meshnetworkscanning_header)
                        .put("product_type", it ?: getString(R.string.default_device_name))
                        .format()
            })

            root.action_create_new_network.visibility = View.GONE
        }
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
        flowManagerVM.flowManager!!.meshSetupModule.updateSelectedMeshNetworkToJoin(networkInfo)
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

