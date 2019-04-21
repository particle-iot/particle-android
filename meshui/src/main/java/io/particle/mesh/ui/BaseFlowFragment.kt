package io.particle.mesh.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.particle.mesh.common.android.livedata.AbsentLiveData
import io.particle.mesh.common.android.livedata.castAndSetOnMainThread
import io.particle.mesh.common.android.livedata.liveDataWithInitialValue
import io.particle.mesh.common.android.livedata.switchMap
import io.particle.mesh.setup.flow.*
import io.particle.mesh.ui.utils.getResOrEmptyString
import io.particle.mesh.ui.utils.getViewModel


data class TitleBarOptions(
    @StringRes val titleRes: Int? = null,
    val showBackButton: Boolean = false,
    val showCloseButton: Boolean = true
)


interface TitleBarOptionsListener {
    fun setTitleBarOptions(options: TitleBarOptions)
}


abstract class BaseFlowFragment : Fragment() {

    open val titleBarOptions = TitleBarOptions()

    val flowUiListener: FlowRunnerUiListener?
        get() = flowRunner.listener

    lateinit var flowScopes: Scopes
    lateinit var flowRunner: MeshFlowRunner
    lateinit var flowSystemInterface: FlowRunnerSystemInterface

    private val onActivityCreatedCalled: LiveData<Boolean?> = liveDataWithInitialValue(false)
    private val onViewCreatedCalled: LiveData<Boolean?> = liveDataWithInitialValue(false)
    private var onFragmentReadyCalled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // make a LiveData which notifies when both onActivityCreated AND onViewCreated
        // have been called, no matter what the order they're called in
        onActivityCreatedCalled.switchMap {
            if (it == true) onViewCreatedCalled else AbsentLiveData()
        }.observe(this, Observer {
            if (it == true && !onFragmentReadyCalled) {
                onFragmentReadyCalled = true
                onFragmentReady(activity!!, flowUiListener!!)
            }
        })
    }

    @CallSuper
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val flowModel: FlowRunnerAccessModel = this.getViewModel()

        flowRunner = flowModel.flowRunner
        flowSystemInterface = flowModel.systemInterface
        flowScopes = flowSystemInterface.scopes

        onActivityCreatedCalled.castAndSetOnMainThread(true)
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onViewCreatedCalled.castAndSetOnMainThread(true)
    }

    override fun onResume() {
        super.onResume()
        val listener = (activity as TitleBarOptionsListener)
        listener.setTitleBarOptions(titleBarOptions)
    }

    open fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        // no-op -- this is for subclasses to use.
    }

    fun getUserFacingTypeName(): String {
        val userFacingDeviceTypeName = flowUiListener?.targetDevice?.deviceType?.toUserFacingName()
        return getResOrEmptyString(userFacingDeviceTypeName)
    }
}
