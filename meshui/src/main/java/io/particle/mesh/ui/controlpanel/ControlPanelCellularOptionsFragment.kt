package io.particle.mesh.ui.controlpanel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.particle.sdk.app.R
import io.particle.sdk.app.R.string


class ControlPanelCellularOptionsFragment : BaseControlPanelFragment() {

    override val titleBarOptions =
        TitleBarOptions(string.p_cp_cellular)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_controlpanel_cellular_options, container, false)
    }

}