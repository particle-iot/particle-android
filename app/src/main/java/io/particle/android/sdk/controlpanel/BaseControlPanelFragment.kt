package io.particle.android.sdk.controlpanel

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import io.particle.mesh.setup.flow.MeshFlowRunner
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.modules.FlowRunnerUiResponseReceiver
import io.particle.sdk.app.R


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