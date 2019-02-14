package io.particle.mesh.setup.ui.ota


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import io.particle.mesh.R
import io.particle.mesh.setup.ui.BaseMeshSetupFragment
import kotlinx.android.synthetic.main.fragment_ble_ota.*


data class BleOtaProgressModel(
    val fileNumber: Int,
    val percentComplete: Int
)


class BleOtaFragment : BaseMeshSetupFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ble_ota, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val deviceModule = flowManagerVM.flowManager?.deviceModule
        deviceModule?.bleUpdateProgress?.observe(
            this,
            Observer {
                it?.apply {
                    render(BleOtaProgressModel(deviceModule.firmwareUpdateCount, it))
                }
            }
        )
    }

    private fun render(model: BleOtaProgressModel) {
        p_bleota_current_file_progress.progress = model.percentComplete
        p_bleota_progress_text.text = getString(
            R.string.p_bleota_progress_format, model.fileNumber, model.percentComplete
        )
    }


}
