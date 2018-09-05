package io.particle.mesh.setup.ui

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.mesh.bluetooth.connecting.BluetoothConnectionManager
import io.particle.mesh.common.android.livedata.setOnMainThread
import io.particle.mesh.setup.connection.ProtocolTransceiverFactory
import io.particle.mesh.setup.connection.security.SecurityManager
import io.particle.mesh.setup.flow.FlowManager
import io.particle.sdk.app.R
import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging


class MeshSetupActivity : AppCompatActivity() {

    private val log = KotlinLogging.logger {}

    private val navController: NavController
        get() = findNavController(R.id.main_nav_host_fragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // do this to make sure we're always providing the correct NavController
        FlowManagerAccessModel.getViewModel(this).setNavController(navController)
    }

    override fun onDestroy() {
        FlowManagerAccessModel.getViewModel(this).setNavController(null)
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }

}



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

    var flowManager: FlowManager? = null

    private val securityManager = SecurityManager()
    private val btConnManager = BluetoothConnectionManager(app)
    private val protocolFactory = ProtocolTransceiverFactory(securityManager)
    private val cloud = ParticleCloudSDK.getCloud()

    private var navReference = MutableLiveData<NavController?>()


    fun startFlowForDevice(deviceType: ParticleDeviceType) {
        resetState()
        flowManager = FlowManager(deviceType, cloud, navReference, btConnManager, protocolFactory)
        flowManager?.startFlow()
    }

    fun setNavController(navController: NavController?) {
        navReference.setOnMainThread(navController)
    }

    override fun onCleared() {
        super.onCleared()
        resetState()
        setNavController(null)
    }

    private fun resetState() {
        flowManager?.clearState()
        flowManager = null
    }
}