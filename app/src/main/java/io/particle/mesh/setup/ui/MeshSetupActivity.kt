package io.particle.mesh.setup.ui

import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.ui.BaseActivity
import io.particle.mesh.bluetooth.btAdapter
import io.particle.mesh.bluetooth.connecting.BluetoothConnectionManager
import io.particle.mesh.common.android.livedata.ClearValueOnInactiveLiveData
import io.particle.mesh.common.android.livedata.castAndPost
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.setOnMainThread
import io.particle.mesh.setup.connection.ProtocolTransceiverFactory
import io.particle.mesh.setup.connection.security.SecurityManager
import io.particle.mesh.setup.flow.FlowManager
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.activity_main.*
import mu.KotlinLogging


private const val REQUEST_ENABLE_BT = 42


class MeshSetupActivity : ProgressHack, BaseActivity() {

    private val log = KotlinLogging.logger {}

    private lateinit var flowVM: FlowManagerAccessModel

    private val navController: NavController
        get() = findNavController(R.id.main_nav_host_fragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // do this to make sure we're always providing the correct NavController
        flowVM = FlowManagerAccessModel.getViewModel(this)
        flowVM.setNavController(navController)
        flowVM.setProgressHack(this)
        flowVM.dialogRequestLD.nonNull().observe(this, Observer { onDialogSpecReceived(it) })

        p_meshactivity_username.text = ParticleCloudSDK.getCloud().loggedInUsername
        p_action_close.setOnClickListener { showCloseSetupConfirmation() }
    }

    override fun onPostResume() {
        super.onPostResume()
        if (!btAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    override fun onDestroy() {
        flowVM.setNavController(null)
        flowVM.setProgressHack(null)
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
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

    override fun showGlobalProgressSpinner(show: Boolean) {
        runOnUiThread { p_mesh_globalProgressSpinner.isVisible = show }
    }

    private fun showCloseSetupConfirmation() {
        MaterialDialog.Builder(this)
                .content(R.string.p_exitsetupconfirmation_content)
                .positiveText(R.string.p_exitsetupconfirmation_exit)
                .negativeText(android.R.string.cancel)
                .onPositive { _, _ -> finish() }
                .show()
    }

    private fun onDialogSpecReceived(spec: DialogSpec?) {
        log.debug { "onDialogSpecReceived()" }
        if (spec == null) {
            log.warn { "Got null dialog spec?!" }
            return
        }
        flowVM.flowManager?.clearDialogRequest()


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
                        flowVM.flowManager!!.updateDialogResult(DialogResult.NEGATIVE)
                    }
                }

                spec.title?.let { builder.title(it) }
            }
        }

        builder.canceledOnTouchOutside(false)
                .onPositive { dialog, _ ->
                    dialog.dismiss()
                    flowVM.flowManager!!.updateDialogResult(DialogResult.POSITIVE)
                }

        log.info { "Showing dialog for: $spec" }
        builder.show()
    }
}

//p_mesh_globalProgressSpinner
interface ProgressHack {
    fun showGlobalProgressSpinner(show: Boolean)
}


enum class DialogResult {
    POSITIVE,
    NEGATIVE,
}


sealed class DialogSpec {

    data class ResDialogSpec(
            @StringRes val text: Int,
            @StringRes val positiveText: Int,
            @StringRes val negativeText: Int? = null,
            @StringRes val title: Int? = null
    ) : DialogSpec()

    data class StringDialogSpec(
            val text: String
    ) : DialogSpec()

}


class FlowManagerAccessModel(private val app: Application) : AndroidViewModel(app) {

    companion object {

        fun getViewModel(activity: androidx.fragment.app.FragmentActivity): FlowManagerAccessModel {
            return ViewModelProviders.of(activity).get(FlowManagerAccessModel::class.java)
        }

        fun getViewModel(fragment: androidx.fragment.app.Fragment): FlowManagerAccessModel {
            val activity = fragment.requireActivity()
            return getViewModel(activity)
        }
    }

    val dialogRequestLD: LiveData<DialogSpec?> = ClearValueOnInactiveLiveData<DialogSpec>()
    var flowManager: FlowManager? = null

    private val securityManager = SecurityManager()
    private val btConnManager = BluetoothConnectionManager(app)
    private val protocolFactory = ProtocolTransceiverFactory(securityManager)
    private val cloud = ParticleCloudSDK.getCloud()

    private val navReference = MutableLiveData<NavController?>()
    private val progressHackReference = MutableLiveData<ProgressHack?>()

    private val log = KotlinLogging.logger {}

    fun startFlowForDevice(deviceType: ParticleDeviceType) {
        if (flowManager == null) {
            flowManager = FlowManager(
                    deviceType,
                    cloud,
                    navReference,
                    dialogRequestLD,
                    ClearValueOnInactiveLiveData<DialogResult>(),
                    btConnManager,
                    protocolFactory,
                    progressHackReference
            )
        }
        flowManager?.startNewFlow()
    }

    fun setNavController(navController: NavController?) {
        navReference.setOnMainThread(navController)
    }

    fun setProgressHack(progressHack: ProgressHack?) {
        progressHackReference.setOnMainThread(progressHack)
    }

    override fun onCleared() {
        super.onCleared()
        log.info { "onCleared()" }
        resetState()
        setNavController(null)
    }

    private fun resetState() {
        flowManager?.clearState()
        flowManager = null
    }
}
