package io.particle.mesh.setup.ui.ota


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.particle.mesh.setup.ui.BaseMeshSetupFragment
import io.particle.sdk.app.R


class BleOtaIntroFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_ble_ota_intro, container, false)
    }


}
