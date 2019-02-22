package io.particle.mesh.setup.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.squareup.phrase.Phrase
import io.particle.mesh.setup.flow.modules.device.NetworkSetupType
import io.particle.mesh.R
import kotlinx.android.synthetic.main.fragment_use_standalone_or_in_mesh.*


class UseStandaloneOrInMeshFragment : BaseMeshSetupFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_use_standalone_or_in_mesh, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fm = flowManagerVM.flowManager!!

        setup_header_text.text = Phrase.from(view, R.string.p_usestandaloneorinmesh_header)
            .put("product_type", fm.getTypeName())
            .format()

        p_usestandaloneorinmesh_subheader.text =
                Phrase.from(view, R.string.p_usestandaloneorinmesh_subheader)
                    .put("product_type", fm.getTypeName())
                    .format()

        p_action_use_in_mesh_network.setOnClickListener {
            findNavController().navigate(R.id.action_global_scanForMeshNetworksFragment)
        }

        p_action_do_not_use_in_mesh_network.setOnClickListener {
            fm.deviceModule.updateNetworkSetupType(NetworkSetupType.STANDALONE)
        }
    }

}
