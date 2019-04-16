package io.particle.android.sdk.controlpanel

import android.os.Bundle
import androidx.fragment.app.Fragment
import io.particle.mesh.setup.flow.MeshFlowRunner
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.modules.FlowRunnerUiResponseReceiver


abstract class BaseFlowFragment : Fragment() {

    val responseReceiver: FlowRunnerUiResponseReceiver?
        get() = flowRunner.responseReceiver

    lateinit var flowScopes: Scopes
    lateinit var flowRunner: MeshFlowRunner
    lateinit var flowSystemInterface: FlowRunnerSystemInterface

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val flowModel = FlowRunnerAccessModel.getViewModel(this)

        flowRunner = flowModel.flowRunner
        flowSystemInterface = flowModel.systemInterface
        flowScopes = flowSystemInterface.scopes
    }

}