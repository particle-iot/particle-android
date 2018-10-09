package io.particle.mesh.setup.ui.ota


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.particle.mesh.setup.ui.BaseMeshSetupFragment
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_ble_ota.*


data class BleOtaProgressModel(
        val fileNumber: Int,
        val percentComplete: Int
)


class BleOtaFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_ble_ota, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        render(BleOtaProgressModel(4, 66))
    }

    fun render(model: BleOtaProgressModel) {
        p_bleota_current_file_progress.progress = model.percentComplete
        p_bleota_progress_text.text = getString(
                R.string.p_bleota_progress_format, model.fileNumber, model.percentComplete
        )
    }


}
