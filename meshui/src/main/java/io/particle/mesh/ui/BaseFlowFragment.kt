package io.particle.mesh.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.snakydesign.livedataextensions.first
import com.snakydesign.livedataextensions.liveDataOf
import com.snakydesign.livedataextensions.switchMap
import io.particle.mesh.common.android.livedata.AbsentLiveData
import io.particle.mesh.common.android.livedata.castAndSetOnMainThread
import io.particle.mesh.setup.flow.*
import io.particle.mesh.ui.utils.getResOrEmptyString
import io.particle.mesh.ui.utils.getViewModel
import mu.KotlinLogging


data class TitleBarOptions(
    @StringRes val titleRes: Int? = null,
    val showBackButton: Boolean = false,
    val showCloseButton: Boolean = true
)


interface TitleBarOptionsListener {
    fun setTitleBarOptions(options: TitleBarOptions)
}


abstract class BaseFlowFragment : Fragment() {

    private val log = KotlinLogging.logger {}


    open val titleBarOptions = TitleBarOptions()

    val flowUiListener: FlowRunnerUiListener?
        get() = flowRunner.listener

    lateinit var flowScopes: Scopes
    lateinit var flowRunner: MeshFlowRunner
    lateinit var flowSystemInterface: FlowRunnerSystemInterface

    @CallSuper
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val flowModel: FlowRunnerAccessModel = this.getViewModel()

        flowRunner = flowModel.flowRunner
        flowSystemInterface = flowModel.systemInterface
        flowScopes = flowSystemInterface.scopes
    }

    override fun onPause() {
        super.onPause()
        log.info { "Paused fragment: ${this::class.java.simpleName}" }
    }

    override fun onResume() {
        super.onResume()
        val listener = (activity as TitleBarOptionsListener)
        listener.setTitleBarOptions(titleBarOptions)
        log.info { "Resumed fragment: ${this::class.java.simpleName}" }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        onFragmentReady(activity!!, flowUiListener!!)
    }

    open fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        // no-op -- this is for subclasses to use.
    }

    override fun onDestroy() {
        super.onDestroy()
        log.info { "Destroyed fragment: ${this::class.java.simpleName}" }
    }

    fun getUserFacingTypeName(): String {
        val userFacingDeviceTypeName = flowUiListener?.targetDevice?.deviceType?.toUserFacingName()
        return getResOrEmptyString(userFacingDeviceTypeName)
    }
}
