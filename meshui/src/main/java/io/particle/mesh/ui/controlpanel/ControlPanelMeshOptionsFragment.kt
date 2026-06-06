package io.particle.mesh.ui.controlpanel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.navigateOnClick
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import io.particle.mesh.ui.databinding.FragmentControlpanelMeshNetworkOptionsBinding


class ControlPanelMeshOptionsFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(
        R.string.p_common_mesh,
        showBackButton = true
    )

    private var _binding: FragmentControlpanelMeshNetworkOptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentControlpanelMeshNetworkOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)
        binding.pControlpanelMeshAddToNetworkFrame.setOnClickListener { addToMesh() }
    }

    private fun addToMesh() {
        flowScopes.onMain { startFlowWithBarcode(flowRunner::startControlPanelMeshAddToMeshFlow) }
    }

}