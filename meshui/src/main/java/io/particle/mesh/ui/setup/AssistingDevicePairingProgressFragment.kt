package io.particle.mesh.ui.setup


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.R


class AssistingDevicePairingProgressFragment : BaseFlowFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_assisting_device_pairing_progress,
            container,
            false
        )
    }
}
