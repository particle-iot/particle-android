package io.particle.mesh.ui.setup


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.R
import io.particle.mesh.ui.databinding.FragmentNewMeshNetworkFinishedBinding


class NewMeshNetworkFinishedFragment : BaseFlowFragment() {

    private var _binding: FragmentNewMeshNetworkFinishedBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentNewMeshNetworkFinishedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        binding.actionAddNextMeshDevice.setOnClickListener {
            flowRunner.startNewFlowWithCommissioner()
        }
        binding.actionStartBuilding.setOnClickListener { endSetup() }

    }

    private fun endSetup() {
        // FIXME: this shouldn't live in the UI
        findNavController().navigate(R.id.action_global_letsGetBuildingFragment)
    }

}
