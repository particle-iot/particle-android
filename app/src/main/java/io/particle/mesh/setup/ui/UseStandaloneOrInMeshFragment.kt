package io.particle.mesh.setup.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.particle.mesh.setup.flow.modules.device.NetworkSetupType
import io.particle.sdk.app.R
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

        val deviceModule = flowManagerVM.flowManager?.deviceModule!!

        p_action_use_in_mesh_network.setOnClickListener {
            deviceModule.updateNetworkSetupType(NetworkSetupType.AS_GATEWAY)
        }

        p_action_do_not_use_in_mesh_network.setOnClickListener {
            deviceModule.updateNetworkSetupType(NetworkSetupType.STANDALONE)
        }
    }

}
