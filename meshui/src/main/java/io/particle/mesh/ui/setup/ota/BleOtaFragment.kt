package io.particle.mesh.ui.setup.ota


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.R
import io.particle.mesh.ui.databinding.FragmentBleOtaBinding


data class BleOtaProgressModel(
    val fileNumber: Int,
    val percentComplete: Int
)


class BleOtaFragment : BaseFlowFragment() {

    private var _binding: FragmentBleOtaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBleOtaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        flowUiListener.deviceData.bleUpdateProgress.observe(this, Observer {
            it?.apply {
                render(
                    BleOtaProgressModel(flowUiListener.deviceData.firmwareUpdateCount, it)
                )
            }
        })
    }

    private fun render(model: BleOtaProgressModel) {
        binding.pBleotaCurrentFileProgress.progress = model.percentComplete
        binding.pBleotaProgressText.text = getString(
            R.string.p_bleota_progress_format, model.fileNumber, model.percentComplete
        )
    }


}
