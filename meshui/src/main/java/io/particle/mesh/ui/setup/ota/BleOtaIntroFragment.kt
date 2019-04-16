package io.particle.mesh.ui.setup.ota


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.particle.mesh.ui.setup.BaseMeshSetupFragment
import io.particle.mesh.ui.R


class BleOtaIntroFragment : io.particle.mesh.ui.setup.BaseMeshSetupFragment() {

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
