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
import io.particle.mesh.ui.databinding.FragmentUseStandaloneOrInMeshBinding


class UseStandaloneOrInMeshFragment : BaseFlowFragment() {

    private var _binding: FragmentUseStandaloneOrInMeshBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUseStandaloneOrInMeshBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        val name = getUserFacingTypeName()

        binding.setupHeaderText.text = Phrase.from(view, R.string.p_usestandaloneorinmesh_header)
            .put("product_type", name)
            .format()

        binding.pUsestandaloneorinmeshSubheader.text =
            Phrase.from(view, R.string.p_usestandaloneorinmesh_subheader)
                .put("product_type", name)
                .format()

        binding.pActionUseInMeshNetwork.setOnClickListener {
            findNavController().navigate(R.id.action_global_scanForMeshNetworksFragment)
        }

        binding.pActionDoNotUseInMeshNetwork.setOnClickListener {
            flowUiListener.setNetworkSetupType(NetworkSetupType.STANDALONE)
        }

    }

}
