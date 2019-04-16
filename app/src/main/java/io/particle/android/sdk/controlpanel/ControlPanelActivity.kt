package io.particle.android.sdk.controlpanel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.setup.flow.modules.FlowUiDelegate
import io.particle.mesh.setup.ui.DialogResult
import io.particle.mesh.setup.ui.DialogSpec
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.activity_control_panel.*
import mu.KotlinLogging
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper


private const val EXTRA_DEVICE_ID = "EXTRA_DEVICE_ID"


class ControlPanelActivity : BaseFlowActivity() {

    companion object {
        fun buildIntent(ctx: Context, deviceId: String): Intent {
            return Intent(ctx, ControlPanelActivity::class.java)
                .putExtra(EXTRA_DEVICE_ID, deviceId)
        }
    }


    override val navHostFragmentId: Int = R.id.main_nav_host_fragment
    override val contentViewIdRes: Int = R.layout.activity_control_panel

    override fun buildFlowUiDelegate(systemInterface: FlowRunnerSystemInterface): FlowUiDelegate {
        return ControlPanelFlowUiDelegate(
            systemInterface.navControllerLD,
            application,
            systemInterface.dialogHack,
            systemInterface
        )
    }

    var showCloseButton: Boolean = false
        set(value) {
            field = value
            p_action_close.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

    var showBackButton: Boolean = true
        set(value) {
            field = value
            p_action_back.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

    var titleText: String = ""
        set(value) {
            field = value
            p_title.text = value
        }

    // FIXME: put this value on an Activity-scoped ViewModel?
    internal val deviceId: String
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

    override fun onDestroy() {
        super.onDestroy()
        flowSystemInterface.setNavController(null)
    }

}


