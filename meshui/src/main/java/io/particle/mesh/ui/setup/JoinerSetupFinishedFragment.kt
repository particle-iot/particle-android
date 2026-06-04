package io.particle.mesh.ui.setup


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.R
import io.particle.mesh.ui.databinding.FragmentJoinerSetupFinishedBinding


class JoinerSetupFinishedFragment : BaseFlowFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding = FragmentJoinerSetupFinishedBinding.inflate(inflater, container, false)

        binding.actionStartBuilding.setOnClickListener{ endSetup() }
        binding.actionAddNextMeshDevice.setOnClickListener{ startNewFlow() }

        return binding.root
    }

    private fun endSetup() {
        // FIXME: this should really just call back to the FlowRunnerUiListener
        findNavController().navigate(R.id.action_global_letsGetBuildingFragment)
    }

    private fun startNewFlow() {
        // FIXME: this should really just call back to the FlowRunnerUiListener
        flowRunner.startNewFlowWithCommissioner()
    }
}
