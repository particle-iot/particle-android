package io.particle.mesh.setup.flow.setupsteps

import androidx.annotation.WorkerThread
import io.particle.firmwareprotos.ctrl.Network.InterfaceEntry
import io.particle.firmwareprotos.ctrl.Network.InterfaceType
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.flow.context.NetworkSetupType
import io.particle.mesh.setup.flow.context.NetworkSetupType.AS_GATEWAY
import io.particle.mesh.setup.flow.context.NetworkSetupType.NODE_JOINER
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.context.SetupDevice
import mu.KotlinLogging


class StepControlPanelDetermineFlowAfterPreflow(
    private val flowUi: FlowUiDelegate
) : MeshSetupStep() {

    private val log = KotlinLogging.logger {}

    @WorkerThread
    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.meshNetworkFlowAdded) {
            // we've already completed all this; bail!
            return
        }

        ctxs.currentFlow = determineRemainingFlow(ctxs, scopes)
        throw ExpectedFlowException("Restarting flow to run new steps")
    }

    private suspend fun determineRemainingFlow(
        ctxs: SetupContexts,
        scopes: Scopes
    ): List<FlowType> {
        ensureHasEthernetChecked(ctxs)

        val meshOnly = (ctxs.targetDevice.connectivityType == Gen3ConnectivityType.MESH_ONLY
                && !ctxs.hasEthernet!!)

        if (meshOnly) {
            ctxs.meshNetworkFlowAdded = true
            return listOf(FlowType.CONTROL_PANEL_PREFLOW, FlowType.CONTROL_PANEL_MESH_JOINER_FLOW)
        }

        ctxs.mesh.showNewNetworkOptionInScanner = true
//
//        if (!ctxs.currentFlow.contains(FlowType.INTERNET_CONNECTED_PREFLOW)) {
//            // we're in an internet connected flow.  Run through that flow and come back here.
//            return listOf(FlowType.PREFLOW, FlowType.INTERNET_CONNECTED_PREFLOW)
//        }

        // if we get here, it's time to add the correct mesh network flow type & interface setup type
        return addInterfaceSetupAndMeshNetworkFlows(ctxs, scopes)
    }

    private suspend fun addInterfaceSetupAndMeshNetworkFlows(
        ctxs: SetupContexts,
        scopes: Scopes
    ): List<FlowType> {
        determineNetworkSetupType(ctxs, scopes)

        val networkSetupType = ctxs.device.networkSetupTypeLD.value!!
        if (networkSetupType == NetworkSetupType.NODE_JOINER) {
            ctxs.meshNetworkFlowAdded = true
            return listOf(FlowType.CONTROL_PANEL_PREFLOW, FlowType.CONTROL_PANEL_MESH_JOINER_FLOW)
        }

        val flows = mutableListOf(FlowType.CONTROL_PANEL_PREFLOW)
        flows.add(
            when (networkSetupType) {
                AS_GATEWAY -> FlowType.CONTROL_PANEL_MESH_CREATE_NETWORK_FLOW
                NODE_JOINER -> FlowType.CONTROL_PANEL_MESH_JOINER_FLOW
                else -> throw IllegalStateException("Invalid value")
            }
        )

        ctxs.meshNetworkFlowAdded = true

        return flows
    }

    private suspend fun ensureHasEthernetChecked(ctxs: SetupContexts) {
        if (ctxs.hasEthernet != null) {
            return
        }

        suspend fun fetchInterfaceList(targetDevice: SetupDevice): List<InterfaceEntry> {
            val xceiver = targetDevice.transceiverLD.value!!
            val reply = xceiver.sendGetInterfaceList().throwOnErrorOrAbsent()
            return reply.interfacesList
        }

        val interfaces = fetchInterfaceList(ctxs.targetDevice)
        ctxs.hasEthernet = null != interfaces.firstOrNull { it.type == InterfaceType.ETHERNET }
    }

    private suspend fun determineNetworkSetupType(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.device.networkSetupTypeLD.value != null) {
            return
        }

        ctxs.device.networkSetupTypeLD.nonNull(scopes).runBlockOnUiThreadAndAwaitUpdate(scopes) {
            flowUi.getNetworkSetupType()
        }
        flowUi.showGlobalProgressSpinner(true)
    }

}