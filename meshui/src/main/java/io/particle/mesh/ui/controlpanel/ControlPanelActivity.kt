package io.particle.mesh.ui.controlpanel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.utils.appHasPermission
import io.particle.android.sdk.utils.pass
import io.particle.mesh.setup.flow.FlowRunnerSystemInterface
import io.particle.mesh.setup.flow.FlowTerminationAction
import io.particle.mesh.setup.flow.FlowTerminationAction.NoFurtherAction
import io.particle.mesh.setup.flow.FlowTerminationAction.StartControlPanelAction
import io.particle.mesh.setup.flow.FlowUiDelegate
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.utils.ToastGravity
import io.particle.mesh.setup.utils.safeToast
import io.particle.mesh.ui.BaseFlowActivity
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import io.particle.mesh.ui.TitleBarOptionsListener
import io.particle.mesh.ui.setup.PermissionsFragment
import kotlinx.android.synthetic.main.activity_control_panel.*
import mu.KotlinLogging
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper


private const val EXTRA_DEVICE = "EXTRA_DEVICE"


class ControlPanelActivity : DeviceProvider, TitleBarOptionsListener, PermissionsFragment.Client,
    BaseFlowActivity() {

    companion object {
        fun buildIntent(ctx: Context, device: ParticleDevice): Intent {
            return Intent(ctx, ControlPanelActivity::class.java)
                .putExtra(EXTRA_DEVICE, device)
        }
    }

    override val progressSpinnerViewId: Int = R.id.p_controlpanel_globalProgressSpinner

    override val navHostFragmentId: Int = R.id.main_nav_host_fragment
    override val contentViewIdRes: Int = R.layout.activity_control_panel

    private val log = KotlinLogging.logger {}

    override val device: ParticleDevice by lazy {
        intent.getParcelableExtra(EXTRA_DEVICE) as ParticleDevice
    }

    override fun onFlowTerminated(nextAction: FlowTerminationAction) {
        log.info { "onFlowTerminated()" }

        when (nextAction) {
            is StartControlPanelAction -> {
                throw IllegalArgumentException(
                    "This action not supported by ${this.javaClass.simpleName}"
                )
            }
            is NoFurtherAction -> {
                val navController = findNavController(navHostFragmentId)
                var result = true
                while (result) {
                    log.info { "Popping back stack" }
                    result = navController.popBackStack()
                }
            }
        }
    }


    private val scopes = Scopes()
    private var shouldCheckPermissions = true

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

        showDeviceInfoView(false)

        PermissionsFragment.ensureAttached(this)
    }

    override fun onResume() {
        super.onResume()
        if (shouldCheckPermissions) {
            shouldCheckPermissions = !appHasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            ensureLocationPermission()
        }
    }

    override fun buildFlowUiDelegate(systemInterface: FlowRunnerSystemInterface): FlowUiDelegate {
        return ControlPanelFlowUiDelegate(
            systemInterface.navControllerLD,
            application,
            systemInterface.dialogHack,
            systemInterface,
            systemInterface.meshFlowTerminator
        )
    }

    override fun setTitleBarOptions(options: TitleBarOptions) {
        val title = options.titleRes ?: R.string.single_space
        p_title.text = getString(title)
        p_action_back.visibility = if (options.showBackButton) View.VISIBLE else View.INVISIBLE
        p_action_close.visibility = if (options.showCloseButton) View.VISIBLE else View.INVISIBLE
    }

    override fun onUserAllowedPermission(permission: String) {
        pass
    }

    override fun onUserDeniedPermission(permission: String) {
        safeToast("Permission denied, exiting Control Panel", gravity = ToastGravity.CENTER)
        finish()
    }

    private fun ensureLocationPermission() {
        PermissionsFragment.get(this)!!.ensurePermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun showDeviceInfoView(showDeviceInfoSlider: Boolean) {
        findViewById<View>(R.id.device_info_bottom_sheet).isVisible = showDeviceInfoSlider
    }

}
