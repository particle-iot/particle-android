package io.particle.mesh.ui.setup

import android.content.Context
import android.os.Bundle
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.mesh.common.QATool
import io.particle.mesh.setup.flow.FlowRunnerSystemInterface
import io.particle.mesh.setup.flow.FlowTerminationAction
import io.particle.mesh.setup.flow.FlowTerminationAction.NoFurtherAction
import io.particle.mesh.setup.flow.FlowTerminationAction.StartControlPanelAction
import io.particle.mesh.setup.flow.FlowUiDelegate
import io.particle.mesh.ui.BaseFlowActivity
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import io.particle.mesh.ui.TitleBarOptionsListener
import io.particle.mesh.ui.controlpanel.ControlPanelActivity
import io.particle.mesh.ui.databinding.ActivityMainBinding
import mu.KotlinLogging


class MeshSetupActivity : TitleBarOptionsListener, BaseFlowActivity() {

    var confirmExitingSetup = true

    override val progressSpinnerViewId: Int
        get() = R.id.p_mesh_globalProgressSpinner

    override val navHostFragmentId: Int
        get() = R.id.main_nav_host_fragment

    override val contentViewIdRes: Int
        get() = R.layout.activity_main


    private val log = KotlinLogging.logger {}

    private val binding by lazy {
        ActivityMainBinding.bind(findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0))
    }

    override fun onFlowTerminated(nextAction: FlowTerminationAction) {
        val nextActionFunction = when (nextAction) {
            is NoFurtherAction -> {
                { /* no-op */ }
            }
            is StartControlPanelAction -> {
                {
                    val intent = ControlPanelActivity.buildIntent(this, nextAction.device)
                    startActivity(intent)
                }
            }
        }
        finish()
        nextActionFunction()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.pMeshactivityUsername.text = ParticleCloudSDK.getCloud().loggedInUsername
        binding.pActionClose.setOnClickListener { showCloseSetupConfirmation() }
    }

    override fun onBackPressed() {
        showCloseSetupConfirmation()
    }

    override fun setTitleBarOptions(options: TitleBarOptions) {
        if (options.titleRes != null) {
            QATool.report(IllegalArgumentException("Title text not supported in setup!"))
        }
        if (options.showBackButton) {
            QATool.report(IllegalArgumentException("Back button not yet supported in setup!"))
//            p_action_back.visibility = if (options.showBackButton) View.VISIBLE else View.INVISIBLE
        }
        binding.pActionClose.visibility = if (options.showCloseButton) View.VISIBLE else View.INVISIBLE
    }

    override fun buildFlowUiDelegate(systemInterface: FlowRunnerSystemInterface): FlowUiDelegate {
        return SetupFlowUiDelegate(
            systemInterface.navControllerLD,
            application,
            systemInterface.dialogHack,
            systemInterface,
            systemInterface.meshFlowTerminator
        )
    }

    private fun showCloseSetupConfirmation() {
        if (!confirmExitingSetup) {
            finish()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.p_exitsetupconfirmation_content)
            .setPositiveButton(R.string.p_exitsetupconfirmation_exit) { _, _ -> finish() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

}
