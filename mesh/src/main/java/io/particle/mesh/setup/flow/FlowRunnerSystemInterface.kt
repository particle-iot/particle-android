package io.particle.mesh.setup.flow

import android.app.Application
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.mesh.common.android.livedata.ClearValueOnInactiveLiveData
import io.particle.mesh.common.android.livedata.castAndPost
import io.particle.mesh.common.android.livedata.castAndSetOnMainThread
import io.particle.mesh.setup.ui.ProgressHack
import mu.KotlinLogging


interface NavigationTool {
    fun navigate(@IdRes target: Int)
    fun navigate(@IdRes target: Int, args: Bundle)
    fun popBackStack()
}


class FlowRunnerSystemInterface : ProgressHack {

    private val log = KotlinLogging.logger {}


    lateinit var flowRunner: MeshFlowRunner

    val scopes = Scopes()
    val dialogRequestLD: LiveData<DialogSpec?> = ClearValueOnInactiveLiveData<DialogSpec>()
    val dialogHack = DialogTool(dialogRequestLD, MutableLiveData())
    val shouldShowProgressSpinnerLD: LiveData<Boolean?> = MutableLiveData()
    val shouldTerminateLD: LiveData<Boolean?> = ClearValueOnInactiveLiveData<Boolean>()

    var navControllerLD: LiveData<NavigationTool?> = MutableLiveData()


    fun initialize(flowRunner: MeshFlowRunner) {
        this.flowRunner = flowRunner
    }

    fun terminateSetup() {
        log.info { "terminateSetup()" }
        shouldTerminateLD.castAndPost(true)
    }

    fun setNavController(navController: NavigationTool?) {
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

    val systemInterface = FlowRunnerSystemInterface()
    lateinit var flowRunner: MeshFlowRunner

    var isInitialized = false
        private set

    fun initialize(flowUiDelegate: FlowUiDelegate) {
        flowRunner = buildFlowManager(
            app,
            ParticleCloudSDK.getCloud(),
            systemInterface.dialogHack,
            flowUiDelegate
        )

        systemInterface.initialize(flowRunner)

        isInitialized = true
    }

    override fun onCleared() {
        super.onCleared()
        systemInterface.shutdown()
    }

}