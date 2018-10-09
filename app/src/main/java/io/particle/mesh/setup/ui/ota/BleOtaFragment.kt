package io.particle.mesh.setup.ui.ota


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.particle.mesh.setup.ui.BaseMeshSetupFragment
import io.particle.sdk.app.R


class BleOtaFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_ble_ota, container, false)
    }



}
