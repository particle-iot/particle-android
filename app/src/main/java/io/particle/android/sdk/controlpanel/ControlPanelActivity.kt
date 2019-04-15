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
import io.particle.mesh.setup.ui.DialogResult
import io.particle.mesh.setup.ui.DialogSpec
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.activity_control_panel.*
import mu.KotlinLogging
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper


private const val EXTRA_DEVICE_ID = "EXTRA_DEVICE_ID"


class ControlPanelActivity : AppCompatActivity() {

    companion object {
        fun buildIntent(ctx: Context, deviceId: String): Intent {
            return Intent(ctx, ControlPanelActivity::class.java)
                .putExtra(EXTRA_DEVICE_ID, deviceId)
        }
    }

    private val log = KotlinLogging.logger {}

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

    private val navController: NavController
        get() = findNavController(io.particle.mesh.R.id.main_nav_host_fragment)

    // FIXME: put this value on an Activity-scoped ViewModel
    internal val deviceId: String
        get() = intent.getStringExtra(EXTRA_DEVICE_ID)

    internal lateinit var meshModel: MeshManagerAccessModel

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_control_panel)

        fun initMeshModel() {
            meshModel = MeshManagerAccessModel.getViewModel(this)
            meshModel.setNavController(navController)

            val flowUi = ControlPanelFlowUiDelegate(
                meshModel.navControllerLD,
                application,
                meshModel.dialogHack,
                meshModel
            )

            meshModel.initialize(flowUi)
        }

        initMeshModel()

        p_action_close.setOnClickListener { finish() }
        p_action_back.setOnClickListener {
            if (!navController.navigateUp()) {
                finish()
            }
        }

        // FIXME: subscribe to LiveDatas
        meshModel.dialogRequestLD.nonNull().observe(this, Observer { onDialogSpecReceived(it) })
        meshModel.shouldShowProgressSpinnerLD.nonNull()
            .observe(this, Observer { showGlobalProgressSpinner(it!!) })
    }

    override fun onDestroy() {
        super.onDestroy()
        // this is where we should be nulling out the
        MeshManagerAccessModel.getViewModel(this).setNavController(null)
    }

    private fun showGlobalProgressSpinner(show: Boolean) {
        log.info { "showGlobalProgressSpinner(): $show" }
        runOnUiThread { p_controlpanel_globalProgressSpinner.isVisible = show }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }

    private fun onDialogSpecReceived(spec: DialogSpec?) {
        log.debug { "onDialogSpecReceived(): $spec" }
        if (spec == null) {
            log.warn { "Got null dialog spec?!" }
            return
        }
        meshModel.dialogHack.clearDialogRequest()


        val builder = MaterialDialog.Builder(this)
        when (spec) {
            is DialogSpec.StringDialogSpec? -> {
                builder.content(spec.text)
                    .positiveText(android.R.string.ok)

            }

            is DialogSpec.ResDialogSpec? -> {
                builder.content(spec.text)
                    .positiveText(spec.positiveText)

                spec.negativeText?.let {
                    builder.negativeText(it)
                    builder.onNegative { dialog, _ ->
                        dialog.dismiss()
                        meshModel.dialogHack.updateDialogResult(DialogResult.NEGATIVE)
                    }
                }

                spec.title?.let { builder.title(it) }
            }
        }

        builder.canceledOnTouchOutside(false)
            .onPositive { dialog, _ ->
                dialog.dismiss()
                meshModel.dialogHack.updateDialogResult(DialogResult.POSITIVE)
            }

        log.info { "Showing dialog for: $spec" }
        builder.show()
    }

}


