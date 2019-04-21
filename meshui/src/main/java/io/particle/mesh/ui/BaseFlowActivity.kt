package io.particle.mesh.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.flow.FlowUiDelegate
import io.particle.mesh.ui.utils.getViewModel
import kotlinx.android.synthetic.main.activity_control_panel.*
import mu.KotlinLogging


abstract class BaseFlowActivity : AppCompatActivity() {

    private val log = KotlinLogging.logger {}

    @get:IdRes
    protected abstract val navHostFragmentId: Int

    @get:LayoutRes
    protected abstract val contentViewIdRes: Int

    @get:IdRes
    protected abstract val progressSpinnerViewId: Int

    protected abstract fun buildFlowUiDelegate(
        systemInterface: FlowRunnerSystemInterface
    ): FlowUiDelegate


    protected val navController: NavController
        get() = findNavController(navHostFragmentId)


    protected lateinit var flowModel: FlowRunnerAccessModel
    protected lateinit var flowSystemInterface: FlowRunnerSystemInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(contentViewIdRes)

        flowModel = this.getViewModel()

        if (savedInstanceState != null && !flowModel.isInitialized) {
            log.warn { "Returning to mesh setup after process death is not supported; exiting!" }
            return
        }

        flowSystemInterface = flowModel.systemInterface
        flowSystemInterface.setNavController(NavigationToolImpl(navController))

        flowModel.initialize(buildFlowUiDelegate(flowSystemInterface))

        // FIXME: subscribe to other LiveDatas?
        flowSystemInterface.dialogRequestLD.nonNull()
            .observe(this, Observer { onDialogSpecReceived(it) })

        flowSystemInterface.shouldShowProgressSpinnerLD.nonNull()
            .observe(this, Observer { showGlobalProgressSpinner(it!!) })

        flowSystemInterface.shouldTerminateLD.nonNull()
            .observe(this, Observer { finish() })
    }

    override fun onDestroy() {
        super.onDestroy()
        flowSystemInterface.setNavController(null)
    }

    private fun showGlobalProgressSpinner(show: Boolean) {
        log.info { "showGlobalProgressSpinner(): $show" }
        runOnUiThread { findViewById<View>(progressSpinnerViewId).isVisible = show }
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
        flowSystemInterface.dialogHack.clearDialogRequest()


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
                        flowSystemInterface.dialogHack.updateDialogResult(DialogResult.NEGATIVE)
                    }
                }

                spec.title?.let { builder.title(it) }
            }
        }

        builder.canceledOnTouchOutside(false)
            .onPositive { dialog, _ ->
                dialog.dismiss()
                flowSystemInterface.dialogHack.updateDialogResult(DialogResult.POSITIVE)
            }

        log.info { "Showing dialog for: $spec" }
        builder.show()
    }


}