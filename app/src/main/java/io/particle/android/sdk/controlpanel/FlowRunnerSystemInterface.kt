package io.particle.android.sdk.controlpanel

import android.app.Application
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.mesh.common.android.livedata.ClearValueOnInactiveLiveData
import io.particle.mesh.common.android.livedata.castAndPost
import io.particle.mesh.common.android.livedata.castAndSetOnMainThread
import io.particle.mesh.setup.flow.DialogTool
import io.particle.mesh.setup.flow.MeshFlowRunner
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.buildFlowManager
import io.particle.mesh.setup.flow.modules.FlowUiDelegate
import io.particle.mesh.setup.ui.DialogSpec
import io.particle.mesh.setup.ui.ProgressHack
import mu.KotlinLogging


class FlowRunnerSystemInterface : ProgressHack {

    private val log = KotlinLogging.logger {}


    lateinit var flowRunner: MeshFlowRunner

    val scopes = Scopes()
    val dialogRequestLD: LiveData<DialogSpec?> = ClearValueOnInactiveLiveData<DialogSpec>()
    val dialogHack = DialogTool(dialogRequestLD, MutableLiveData())
    val shouldTerminateLD: LiveData<Boolean?> = ClearValueOnInactiveLiveData<Boolean>()
    val shouldShowProgressSpinnerLD: LiveData<Boolean?> = MutableLiveData()

    var navControllerLD: LiveData<NavController?> = MutableLiveData()


    fun initialize(flowRunner: MeshFlowRunner) {
        this.flowRunner = flowRunner
    }

    fun terminateSetup() {
        log.info { "terminateSetup()" }
        shouldTerminateLD.castAndPost(true)
    }

    fun setNavController(navController: NavController?) {
        navControllerLD.castAndSetOnMainThread(navController)
    }

    override fun showGlobalProgressSpinner(show: Boolean) {
        shouldShowProgressSpinnerLD.castAndPost(show)
    }

    fun shutdown() {
        setNavController(null)
        scopes.cancelAll()
    }
}


// FIXME: this shouldn't have much of anything here -- wrap all this logic/members into a helper class
class FlowRunnerAccessModel(private val app: Application) : AndroidViewModel(app) {

    companion object {

        fun getViewModel(activity: FragmentActivity): FlowRunnerAccessModel {
            return ViewModelProviders.of(activity)
                .get(FlowRunnerAccessModel::class.java)
        }

        fun getViewModel(fragment: Fragment): FlowRunnerAccessModel {
            val activity = fragment.requireActivity()
            return getViewModel(activity)
        }
    }


    val systemInterface = FlowRunnerSystemInterface()
    lateinit var flowRunner: MeshFlowRunner


    fun initialize(flowUiDelegate: FlowUiDelegate) {
        flowRunner = buildFlowManager(
            app,
            ParticleCloudSDK.getCloud(),
            systemInterface.dialogHack,
            flowUiDelegate
        )

        systemInterface.initialize(flowRunner)
    }

    override fun onCleared() {
        super.onCleared()
        systemInterface.shutdown()
    }

}