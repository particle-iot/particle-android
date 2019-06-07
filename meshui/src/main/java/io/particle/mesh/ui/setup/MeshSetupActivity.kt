package io.particle.mesh.ui.setup

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import io.particle.android.common.isLocationServicesAvailable
import io.particle.android.common.promptUserToEnableLocationServices
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.mesh.bluetooth.btAdapter
import io.particle.mesh.common.QATool
import io.particle.mesh.setup.flow.FlowRunnerSystemInterface
import io.particle.mesh.setup.flow.FlowUiDelegate
import io.particle.mesh.ui.BaseFlowActivity
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import io.particle.mesh.ui.TitleBarOptionsListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.p_action_close
import mu.KotlinLogging
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper
import java.lang.IllegalStateException

private const val REQUEST_ENABLE_BT = 42


class MeshSetupActivity : TitleBarOptionsListener, BaseFlowActivity() {

    override val progressSpinnerViewId: Int
        get() = R.id.p_mesh_globalProgressSpinner

    override val navHostFragmentId: Int
        get() = R.id.main_nav_host_fragment

    override val contentViewIdRes: Int
        get() = R.layout.activity_main


    private val log = KotlinLogging.logger {}


    override fun onFlowTerminated() {
        finish()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        p_meshactivity_username.text = ParticleCloudSDK.getCloud().loggedInUsername
        p_action_close.setOnClickListener { showCloseSetupConfirmation() }
    }

    override fun onPostResume() {
        super.onPostResume()
        if (!btAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        if (!isLocationServicesAvailable()) {
            promptUserToEnableLocationServices { finish() }
        }
    }

    override fun onBackPressed() {
        showCloseSetupConfirmation()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            // FIXME: inform the user why we're exiting?
            finish()
        }
    }

    override fun setTitleBarOptions(options: TitleBarOptions) {
        if (options.titleRes != null) {
            QATool.report(IllegalArgumentException("Title text not supported in setup!"))
        }
        if (options.showBackButton) {
            QATool.report(IllegalArgumentException("Back button not yet supported in setup!"))
//            p_action_back.visibility = if (options.showBackButton) View.VISIBLE else View.INVISIBLE
        }
        p_action_close.visibility = if (options.showCloseButton) View.VISIBLE else View.INVISIBLE
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
        MaterialDialog.Builder(this)
            .content(R.string.p_exitsetupconfirmation_content)
            .positiveText(R.string.p_exitsetupconfirmation_exit)
            .negativeText(android.R.string.cancel)
            .onPositive { _, _ -> finish() }
            .show()
    }

}
