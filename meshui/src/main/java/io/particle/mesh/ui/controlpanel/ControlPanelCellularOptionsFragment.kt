package io.particle.mesh.ui.controlpanel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions


class ControlPanelCellularOptionsFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(R.string.p_cp_cellular)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_controlpanel_cellular_options, container, false)
    }

}