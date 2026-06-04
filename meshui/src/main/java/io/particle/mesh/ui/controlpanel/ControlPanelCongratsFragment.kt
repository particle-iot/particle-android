package io.particle.mesh.ui.controlpanel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import io.particle.mesh.ui.databinding.FragmentCpCongratsBinding
import io.particle.mesh.ui.inflateFragment
import kotlinx.coroutines.delay


class ControlPanelCongratsFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(showCloseButton = false)

    private val args: ControlPanelCongratsFragmentArgs by navArgs()

    private var _binding: FragmentCpCongratsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = container?.inflateFragment(R.layout.fragment_cp_congrats)
        _binding = root?.let { FragmentCpCongratsBinding.bind(it) }
        return root
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        binding.pHashtagwinningMessage.text = args.congratsMessage
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}