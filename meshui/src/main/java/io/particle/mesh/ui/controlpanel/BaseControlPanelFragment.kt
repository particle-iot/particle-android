package io.particle.mesh.ui.controlpanel

import android.os.Bundle
import io.particle.mesh.ui.BaseFlowFragment


interface DeviceIdProvider {
    val deviceId: String
}


open class BaseControlPanelFragment : BaseFlowFragment() {

    lateinit var deviceId: String

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        deviceId = (activity!! as ControlPanelActivity).deviceId
    }

}