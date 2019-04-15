package io.particle.android.sdk.controlpanel

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import io.particle.mesh.setup.flow.modules.FlowRunnerUiResponseReceiver
import io.particle.sdk.app.R


data class TitleBarOptions(
    @StringRes val titleRes: Int? = null,
    val showBackButton: Boolean = false,
    val showCloseButton: Boolean = true
)


open class BaseControlPanelFragment : Fragment() {

    lateinit var deviceId: String
    lateinit var meshModel: MeshManagerAccessModel

    open val titleBarOptions = TitleBarOptions()

    val responseReceiver: FlowRunnerUiResponseReceiver?
        get() = meshModel.flowRunner.responseReceiver

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        meshModel = MeshManagerAccessModel.getViewModel(this)
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