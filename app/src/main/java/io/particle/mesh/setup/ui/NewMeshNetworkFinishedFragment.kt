package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.squareup.phrase.Phrase
import io.particle.mesh.common.QATool
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_new_mesh_network_finished.view.*
import java.lang.NullPointerException


class NewMeshNetworkFinishedFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_new_mesh_network_finished, container, false)

        root.action_add_next_mesh_device.setOnClickListener {
            flowManagerVM.flowManager?.startNewFlowWithCommissioner()
        }
        root.action_start_building.setOnClickListener { endSetup() }

        flowManagerVM.flowManager!!.cloudConnectionModule.currentDeviceName.observe(this, Observer {
            if (it == null) {
                QATool.report(NullPointerException("Device name is null."))
            }
            val deviceName = it ?: getString(R.string.default_device_name)

            root.setup_header_text.text = Phrase.from(view, R.string.p_newmeshnetworkfinished_subheader_1)
                    .put("product_type", deviceName)
                    .format()
        })


        return root
    }

    private fun endSetup() {
        requireActivity().finish()
    }

}
