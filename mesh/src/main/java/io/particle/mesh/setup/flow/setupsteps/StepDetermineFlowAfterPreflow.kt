package io.particle.mesh.setup.flow.setupsteps

import androidx.annotation.WorkerThread
import io.particle.firmwareprotos.ctrl.Network.InterfaceEntry
import io.particle.firmwareprotos.ctrl.Network.InterfaceType
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.flow.FlowIntent.FIRST_TIME_SETUP
import io.particle.mesh.setup.flow.FlowIntent.SINGLE_TASK_FLOW
import io.particle.mesh.setup.flow.context.NetworkSetupType
import io.particle.mesh.setup.flow.context.NetworkSetupType.AS_GATEWAY
import io.particle.mesh.setup.flow.context.NetworkSetupType.NODE_JOINER
import io.particle.mesh.setup.flow.context.NetworkSetupType.STANDALONE
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.context.SetupDevice
import mu.KotlinLogging


class StepDetermineFlowAfterPreflow : MeshSetupStep() {

    private val log = KotlinLogging.logger {}

    @WorkerThread
    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        when(ctxs.flowIntent!!) {
            SINGLE_TASK_FLOW -> log.info { "Single task flow; continue." }
            FIRST_TIME_SETUP -> onFirstTimeSetup(ctxs, scopes)
        }
    }

    private suspend fun onFirstTimeSetup(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.meshNetworkFlowAdded) {
            // we've already completed all this; bail!
            return
        }

        ctxs.currentFlow = determineRemainingFlow(ctxs)
        throw ExpectedFlowException("Restarting flow to run new steps")
    }

    private suspend fun determineRemainingFlow(ctxs: SetupContexts): List<FlowType> {
        ensureHasEthernetChecked(ctxs)

        val meshOnly = (ctxs.targetDevice.connectivityType == Gen3ConnectivityType.MESH_ONLY
                && !ctxs.hasEthernet!!)

        if (meshOnly) {
            ctxs.meshNetworkFlowAdded = true
            return listOf(FlowType.JOINER_FLOW)
        }

        if (!ctxs.currentFlow.contains(FlowType.INTERNET_CONNECTED_PREFLOW)) {
            // we're in an internet connected flow.  Run through that flow and come back here.
            ctxs.mesh.showNewNetworkOptionInScanner = true
            return listOf(FlowType.INTERNET_CONNECTED_PREFLOW)
        }

        // if we get here, it's time to add the correct mesh network flow type & interface setup type
        return addInterfaceSetupAndMeshNetworkFlows(ctxs)
    }

    private suspend fun addInterfaceSetupAndMeshNetworkFlows(ctxs: SetupContexts): List<FlowType> {
        if (ctxs.device.networkSetupTypeLD.value!! == NetworkSetupType.NODE_JOINER) {
            return listOf(FlowType.JOINER_FLOW)
        }

        val flows = mutableListOf(getInterfaceSetupFlow(ctxs))
        flows.add(when(ctxs.device.networkSetupTypeLD.value!!) {
            AS_GATEWAY -> FlowType.NETWORK_CREATOR_POSTFLOW
            STANDALONE -> FlowType.STANDALONE_POSTFLOW
            NODE_JOINER -> FlowType.JOINER_FLOW
        })

        return flows
    }

    private fun getInterfaceSetupFlow(ctxs: SetupContexts): FlowType {
        return if (ctxs.hasEthernet!!) {
            FlowType.ETHERNET_FLOW

        } else {
            when(ctxs.targetDevice.connectivityType!!) {
                Gen3ConnectivityType.WIFI -> FlowType.WIFI_FLOW
                Gen3ConnectivityType.CELLULAR -> FlowType.CELLULAR_FLOW
                Gen3ConnectivityType.MESH_ONLY -> FlowType.JOINER_FLOW
            }
        }
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

}