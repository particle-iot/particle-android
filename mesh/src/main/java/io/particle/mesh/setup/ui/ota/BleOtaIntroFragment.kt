package io.particle.mesh.setup.ui.ota


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.particle.mesh.setup.ui.BaseMeshSetupFragment
import io.particle.mesh.R
import kotlinx.android.synthetic.main.fragment_ble_ota_intro.*


class BleOtaIntroFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_ble_ota_intro, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        action_next.setOnClickListener {
            flowManagerVM.flowManager?.deviceModule?.updateUserConsentedToFirmwareUpdate(true)
        }
    }

}
