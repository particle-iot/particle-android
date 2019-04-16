package io.particle.mesh.ui.controlpanel

import android.os.Bundle
import androidx.annotation.StringRes
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.R


data class TitleBarOptions(
    @StringRes val titleRes: Int? = null,
    val showBackButton: Boolean = false,
    val showCloseButton: Boolean = true
)


open class BaseControlPanelFragment : BaseFlowFragment() {

    open val titleBarOptions = TitleBarOptions()

    lateinit var deviceId: String

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        deviceId = (requireActivity() as ControlPanelActivity).deviceId
    }

    override fun onResume() {
        super.onResume()
        val cpActivity = (activity as ControlPanelActivity)
        val title = titleBarOptions.titleRes ?: R.string.single_space
        cpActivity.titleText = getString(title)
        cpActivity.showBackButton = titleBarOptions.showBackButton
        cpActivity.showCloseButton = titleBarOptions.showCloseButton
    }

}