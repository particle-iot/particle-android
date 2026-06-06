package io.particle.mesh.ui.setup.ota


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.databinding.FragmentBleOtaIntroBinding


class BleOtaIntroFragment : BaseFlowFragment() {

    private var _binding: FragmentBleOtaIntroBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentBleOtaIntroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)
        binding.actionNext.setOnClickListener {
            flowUiListener.deviceData.updateUserConsentedToFirmwareUpdate(true)
        }
    }

}
