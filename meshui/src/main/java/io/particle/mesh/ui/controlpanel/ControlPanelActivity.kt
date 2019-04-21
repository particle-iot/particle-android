package io.particle.mesh.ui.controlpanel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import io.particle.mesh.setup.flow.FlowUiDelegate
import io.particle.mesh.ui.BaseFlowActivity
import io.particle.mesh.setup.flow.FlowRunnerSystemInterface
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import io.particle.mesh.ui.TitleBarOptionsListener
import kotlinx.android.synthetic.main.activity_control_panel.*
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper


private const val EXTRA_DEVICE_ID = "EXTRA_DEVICE_ID"


class ControlPanelActivity : DeviceIdProvider, TitleBarOptionsListener, BaseFlowActivity() {

    companion object {
        fun buildIntent(ctx: Context, deviceId: String): Intent {
            return Intent(ctx, ControlPanelActivity::class.java)
                .putExtra(EXTRA_DEVICE_ID, deviceId)
        }
    }

    override val progressSpinnerViewId: Int = R.id.p_controlpanel_globalProgressSpinner

    override val navHostFragmentId: Int = R.id.main_nav_host_fragment
    override val contentViewIdRes: Int = R.layout.activity_control_panel

    override val deviceId: String
        get() = intent.getStringExtra(EXTRA_DEVICE_ID)


    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        p_action_close.setOnClickListener { finish() }
        p_action_back.setOnClickListener {
            if (!navController.navigateUp()) {
                finish()
            }
        }
    }

    override fun buildFlowUiDelegate(systemInterface: FlowRunnerSystemInterface): FlowUiDelegate {
        return ControlPanelFlowUiDelegate(
            systemInterface.navControllerLD,
            application,
            systemInterface.dialogHack,
            systemInterface
        )
    }

    override fun setTitleBarOptions(options: TitleBarOptions) {
        val title = options.titleRes ?: R.string.single_space
        p_title.text = getString(title)
        p_action_back.visibility = if (options.showBackButton) View.VISIBLE else View.INVISIBLE
        p_action_close.visibility = if (options.showCloseButton) View.VISIBLE else View.INVISIBLE
    }

}
