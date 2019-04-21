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
import kotlinx.android.synthetic.main.fragment_ble_ota.*


data class BleOtaProgressModel(
    val fileNumber: Int,
    val percentComplete: Int
)


class BleOtaFragment : BaseFlowFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ble_ota, container, false)
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
        p_bleota_current_file_progress.progress = model.percentComplete
        p_bleota_progress_text.text = getString(
            R.string.p_bleota_progress_format, model.fileNumber, model.percentComplete
        )
    }


}
