package io.particle.mesh.ui

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
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
import com.google.android.material.snackbar.Snackbar
import com.snakydesign.livedataextensions.filter
import com.snakydesign.livedataextensions.nonNull
import io.particle.android.common.isLocationServicesAvailable
import io.particle.android.common.promptUserToEnableLocationServices
import io.particle.mesh.bluetooth.btAdapter
import io.particle.mesh.setup.flow.*
import io.particle.mesh.ui.utils.getViewModel
import mu.KotlinLogging


private const val REQUEST_ENABLE_BT = 42


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

    protected abstract fun onFlowTerminated()

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
            finish()
            return
        }

        flowSystemInterface = flowModel.systemInterface
        flowSystemInterface.setNavController(
            BaseNavigationToolImpl(navController, applicationContext)
        )

        flowModel.initialize(buildFlowUiDelegate(flowSystemInterface))

        // FIXME: subscribe to other LiveDatas?
        flowSystemInterface.dialogRequestLD.nonNull()
            .observe(this, Observer { onDialogSpecReceived(it) })

        flowSystemInterface.snackbarRequestLD.nonNull()
            .observe(this, Observer { onSnackbarRequestReceived(it) })

        flowSystemInterface.shouldShowProgressSpinnerLD.nonNull()
            .observe(this, Observer { showGlobalProgressSpinner(it!!) })

        flowSystemInterface.meshFlowTerminator.shouldTerminateFlowLD
            .filter { it == true }
            .observe(this, Observer { onFlowTerminated() })
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

    override fun onPause() {
        super.onPause()
        log.info { "onPause()" }
        if (isFinishing) {
            flowSystemInterface.shutdown()
            flowSystemInterface.setNavController(null)
            flowModel.shutdown()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            // FIXME: inform the user why we're exiting?
            finish()
        }
    }

    private fun showGlobalProgressSpinner(show: Boolean) {
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

        val strSpec: DialogSpec.StringDialogSpec = when (spec) {
            is DialogSpec.StringDialogSpec? -> spec
            is DialogSpec.ResDialogSpec? -> {
                val negativeText = spec.negativeText?.let { getString(it) }
                val title = spec.title?.let { getString(it) }
                DialogSpec.StringDialogSpec(
                    getString(spec.text),
                    getString(spec.positiveText),
                    negativeText,
                    title
                )
            }
        }

        builder.content(strSpec.text)
            .positiveText(strSpec.positiveText)

        strSpec.negativeText?.let {
            builder.negativeText(it)
            builder.onNegative { dialog, _ ->
                dialog.dismiss()
                flowSystemInterface.dialogHack.updateDialogResult(DialogResult.NEGATIVE)
            }
        }

        strSpec.title?.let { builder.title(it) }

        builder.canceledOnTouchOutside(false)
            .onPositive { dialog, _ ->
                dialog.dismiss()
                flowSystemInterface.dialogHack.updateDialogResult(DialogResult.POSITIVE)
            }

        log.info { "Showing dialog for: $spec" }
        builder.show()
    }

    private fun onSnackbarRequestReceived(message: String?) {
        log.debug { "Got snackbar msg request=$message" }
        if (message == null) {
            log.warn { "Got null snackbar message?!" }
            return
        }

        Snackbar.make(
            findViewById(navHostFragmentId),
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }

}