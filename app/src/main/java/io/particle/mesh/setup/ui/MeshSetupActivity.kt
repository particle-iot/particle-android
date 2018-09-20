package io.particle.mesh.setup.ui

import android.app.Application
import android.arch.lifecycle.*
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.mesh.bluetooth.connecting.BluetoothConnectionManager
import io.particle.mesh.common.android.livedata.ClearValueOnInactiveLiveData
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.setOnMainThread
import io.particle.mesh.setup.connection.ProtocolTransceiverFactory
import io.particle.mesh.setup.connection.security.SecurityManager
import io.particle.mesh.setup.flow.FlowManager
import io.particle.sdk.app.R
import mu.KotlinLogging
import kotlinx.android.synthetic.main.activity_main.*


class MeshSetupActivity : AppCompatActivity() {

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
        flowVM.dialogRequestLD.observe(this, Observer { onDialogSpecReceived(it) })

        p_meshactivity_username.text = ParticleCloudSDK.getCloud().loggedInUsername
        p_action_close.setOnClickListener { showCloseSetupConfirmation() }
    }

    override fun onDestroy() {
        FlowManagerAccessModel.getViewModel(this).setNavController(null)
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }

    override fun onBackPressed() {
//        super.onBackPressed()
        showCloseSetupConfirmation(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            // FIXME: inform the user why we're exiting?
            finish()
        }
    }

    private fun showCloseSetupConfirmation(fromBackPress: Boolean = false) {
        MaterialDialog.Builder(this)
                .content(R.string.p_exitsetupconfirmation_content)
                .positiveText(R.string.p_exitsetupconfirmation_exit)
                .negativeText(android.R.string.cancel)
                .onPositive { _, _ ->
                    if (fromBackPress) {
                        super.onBackPressed()
                    } else {
                        finish()
                    }
                }
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
                .content(spec.text)
                .canceledOnTouchOutside(false)
                .positiveText(spec.positiveText)
                .onPositive { dialog, _ ->
                    dialog.dismiss()
                    flowVM.flowManager!!.updateDialogResult(DialogResult.POSITIVE)
                }

        spec.negativeText?.let {
            builder.negativeText(it)
            builder.onNegative { dialog, _ ->
                dialog.dismiss()
                flowVM.flowManager!!.updateDialogResult(DialogResult.NEGATIVE)
            }
        }

        spec.title?.let { builder.title(it) }

        log.info { "Showing dialog for: $spec" }
        builder.show()
    }
}


enum class DialogResult {
    POSITIVE,
    NEGATIVE,
}


data class DialogSpec(
        @StringRes val text: Int,
        @StringRes val positiveText: Int,
        @StringRes val negativeText: Int? = null,
        @StringRes val title: Int? = null
)


class FlowManagerAccessModel(app: Application) : AndroidViewModel(app) {

    companion object {

        fun getViewModel(activity: FragmentActivity): FlowManagerAccessModel {
            return ViewModelProviders.of(activity).get(FlowManagerAccessModel::class.java)
        }

        fun getViewModel(fragment: Fragment): FlowManagerAccessModel {
            val activity = fragment.requireActivity()
            return getViewModel(activity)
        }
    }

    val dialogRequestLD: LiveData<DialogSpec?> = ClearValueOnInactiveLiveData<DialogSpec>().nonNull()
    var flowManager: FlowManager? = null

    private val securityManager = SecurityManager()
    private val btConnManager = BluetoothConnectionManager(app)
    private val protocolFactory = ProtocolTransceiverFactory(securityManager)
    private val cloud = ParticleCloudSDK.getCloud()

    private val navReference = MutableLiveData<NavController?>()

    private val log = KotlinLogging.logger {}

    fun startFlowForDevice(deviceType: ParticleDeviceType) {
        if (flowManager == null) {
            flowManager = FlowManager(
                    deviceType,
                    cloud,
                    navReference,
                    dialogRequestLD,
                    ClearValueOnInactiveLiveData<DialogResult>().nonNull(),
                    btConnManager,
                    protocolFactory
            )
        }
        flowManager?.startNewFlow()
    }

    fun setNavController(navController: NavController?) {
        navReference.setOnMainThread(navController)
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
