package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_new_mesh_network_name.view.*


class NewMeshNetworkNameFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_new_mesh_network_name, container, false)
        root.action_next.setOnClickListener { onNetworkNameEntered() }
        return root
    }

    private fun onNetworkNameEntered() {
        validateNetworkName()
        // FIXME: update network name LD
    }

    private fun validateNetworkName() {
        // FIXME: IMPLEMENT!
    }
}
