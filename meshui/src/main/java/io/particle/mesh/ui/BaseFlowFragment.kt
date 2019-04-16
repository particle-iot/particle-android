package io.particle.mesh.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import io.particle.mesh.setup.flow.FlowRunnerAccessModel
import io.particle.mesh.setup.flow.FlowRunnerSystemInterface
import io.particle.mesh.setup.flow.MeshFlowRunner
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.FlowRunnerUiResponseReceiver
import io.particle.mesh.ui.utils.getViewModel


abstract class BaseFlowFragment : Fragment() {

    val responseReceiver: FlowRunnerUiResponseReceiver?
        get() = flowRunner.responseReceiver

    lateinit var flowScopes: Scopes
    lateinit var flowRunner: MeshFlowRunner
    lateinit var flowSystemInterface: FlowRunnerSystemInterface

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val flowModel: FlowRunnerAccessModel = this.getViewModel()

        flowRunner = flowModel.flowRunner
        flowSystemInterface = flowModel.systemInterface
        flowScopes = flowSystemInterface.scopes
    }

}