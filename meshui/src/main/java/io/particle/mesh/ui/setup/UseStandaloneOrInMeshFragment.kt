package io.particle.mesh.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import com.squareup.phrase.Phrase
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.setup.flow.context.NetworkSetupType
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.R
import kotlinx.android.synthetic.main.fragment_use_standalone_or_in_mesh.*


class UseStandaloneOrInMeshFragment : BaseFlowFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_use_standalone_or_in_mesh, container, false)
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        val name = getUserFacingTypeName()

        setup_header_text.text = Phrase.from(view, R.string.p_usestandaloneorinmesh_header)
            .put("product_type", name)
            .format()

        p_usestandaloneorinmesh_subheader.text =
            Phrase.from(view, R.string.p_usestandaloneorinmesh_subheader)
                .put("product_type", name)
                .format()

        p_action_use_in_mesh_network.setOnClickListener {
            findNavController().navigate(R.id.action_global_scanForMeshNetworksFragment)
        }

        p_action_do_not_use_in_mesh_network.setOnClickListener {
            flowUiListener.setNetworkSetupType(NetworkSetupType.STANDALONE)
        }

    }

}
