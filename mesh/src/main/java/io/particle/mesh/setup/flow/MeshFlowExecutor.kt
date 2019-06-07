package io.particle.mesh.setup.flow

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.snakydesign.livedataextensions.nonNull
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.mesh.common.QATool
import io.particle.mesh.common.android.livedata.awaitUpdate
import io.particle.mesh.ota.FirmwareUpdateManager
import io.particle.mesh.setup.flow.DialogSpec.StringDialogSpec
import io.particle.mesh.setup.flow.ExceptionType.ERROR_FATAL
import io.particle.mesh.setup.flow.ExceptionType.EXPECTED_FLOW
import io.particle.mesh.setup.flow.FlowType.CELLULAR_FLOW
import io.particle.mesh.setup.flow.FlowType.CONTROL_PANEL_CELLULAR_PRESENT_OPTIONS_FLOW
import io.particle.mesh.setup.flow.FlowType.CONTROL_PANEL_CELLULAR_SET_NEW_DATA_LIMIT
import io.particle.mesh.setup.flow.FlowType.CONTROL_PANEL_CELLULAR_SIM_ACTION_POSTFLOW
import io.particle.mesh.setup.flow.FlowType.CONTROL_PANEL_CELLULAR_SIM_DEACTIVATE
import io.particle.mesh.setup.flow.FlowType.CONTROL_PANEL_CELLULAR_SIM_REACTIVATE
import io.particle.mesh.setup.flow.FlowType.CONTROL_PANEL_CELLULAR_SIM_UNPAUSE
import io.particle.mesh.setup.flow.FlowType.CONTROL_PANEL_MESH_INSPECT_NETWORK_FLOW
import io.particle.mesh.setup.flow.FlowType.CONTROL_PANEL_MESH_LEAVE_NETWORK_FLOW
import io.particle.mesh.setup.flow.FlowType.CONTROL_PANEL_WIFI_INSPECT_NETWORK_FLOW
import io.particle.mesh.setup.flow.FlowType.ETHERNET_FLOW
import io.particle.mesh.setup.flow.FlowType.INTERNET_CONNECTED_PREFLOW
import io.particle.mesh.setup.flow.FlowType.JOINER_FLOW
import io.particle.mesh.setup.flow.FlowType.NETWORK_CREATOR_POSTFLOW
import io.particle.mesh.setup.flow.FlowType.PREFLOW
import io.particle.mesh.setup.flow.FlowType.SINGLE_TASK_POSTFLOW
import io.particle.mesh.setup.flow.FlowType.STANDALONE_POSTFLOW
import io.particle.mesh.setup.flow.FlowType.WIFI_FLOW
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.setupsteps.*
import kotlinx.coroutines.delay
import mu.KotlinLogging


private const val FLOW_RETRIES = 5


class StepDeps(
    val cloud: ParticleCloud,
    val deviceConnector: DeviceConnector,
    val firmwareUpdateManager: FirmwareUpdateManager,
    val dialogTool: DialogTool,
    val flowUi: FlowUiDelegate
)


class MeshFlowExecutor(
    private val deps: StepDeps,
    private val flowTerminator: MeshFlowTerminator
) {

    private val log = KotlinLogging.logger {}


    // set up a placeholder listener that will be overwritten when a new flow starts
    var listener: FlowRunnerUiListener = FlowRunnerUiListener(SetupContexts())

    var contexts: SetupContexts? = null
        private set


    @MainThread
    fun executeNewFlow(
        intent: FlowIntent,
        flowsToRun: List<FlowType>,
        preInitializedContext: SetupContexts? = null
    ) {
        initNewFlow(intent, preInitializedContext)

        contexts?.currentFlow = flowsToRun

        contexts?.scopes?.onWorker {
            runCurrentFlow()
        }
    }

    fun endSetup() {
        log.info { "endSetup()" }
        flowTerminator.terminateFlow()
    }

    private fun initNewFlow(intent: FlowIntent, preInitializedContext: SetupContexts?) {
        val ctxs = preInitializedContext ?: SetupContexts()
        contexts = ctxs
        listener = FlowRunnerUiListener(ctxs)
        ctxs.flowIntent = intent
    }

    @WorkerThread
    private fun runCurrentFlow() {
        log.info { "runCurrentFlow()" }

        fun assembleSteps(ctxs: SetupContexts): List<MeshSetupStep> {
            val flow = ctxs.currentFlow.toMutableList()
            log.info { "assembleSteps(), steps=$flow" }
            val steps = mutableListOf<MeshSetupStep>()
            for (type in flow) {
                val newSteps = getFlowSteps(type)
                steps.addAll(newSteps)
            }
            return steps
        }

        suspend fun doRunFlow(flowSteps: List<MeshSetupStep>) {
            for (step in flowSteps) {
                contexts?.let {
                    step.runStep(it, it.scopes)
                }
            }
        }

        val ctxs = contexts
        ctxs?.scopes?.onWorker {
            var error: Exception? = null

            var i = 0
            while (i < FLOW_RETRIES) {
                try {
                    val steps = assembleSteps(ctxs)
                    doRunFlow(steps)
                    log.info { "FLOW COMPLETED SUCCESSFULLY!" }
                    return@onWorker

                } catch (ex: Exception) {

                    if (ex is MeshSetupFlowException && ex.severity == EXPECTED_FLOW) {
                        log.info { "Received EXPECTED_FLOW exception; retrying." }
                        continue  // avoid incrementing the counter, since this was expected flow
                    }

                    if (ex is MeshSetupFlowException && ex.severity == ERROR_FATAL) {
                        log.info(ex) { "Hit fatal error, exiting setup: " }
                        QATool.log(ex.message ?: "(no message)")
                        quitSetupfromError(ctxs.scopes, ex)
                        return@onWorker
                    }

                    delay(1000)
                    QATool.report(ex)
                    error = ex

                    i++
                }
            }

            // we got through all the retries and we finally failed on a specific error.
            // Quit and notify the user of the error we died on
            quitSetupfromError(ctxs.scopes, error)
        }
    }

    private suspend fun quitSetupfromError(scopes: Scopes, ex: Exception?) {
        val preMsg = ex?.message ?: "Setup has encountered an error and cannot continue."
        scopes.withMain {
            deps.dialogTool.newDialogRequest(StringDialogSpec(preMsg))
            deps.dialogTool.clearDialogResult()
            deps.dialogTool.dialogResultLD.nonNull().awaitUpdate(scopes)
            endSetup()
        }
    }

    private fun getFlowSteps(flowType: FlowType): List<MeshSetupStep> {

        return when (flowType) {

            PREFLOW -> listOf(
                StepGetTargetDeviceInfo(deps.flowUi),
                StepShowGetReadyForSetup(deps.flowUi),
                StepConnectToTargetDevice(deps.flowUi, deps.deviceConnector),
                StepEnsureCorrectEthernetFeatureStatus(),
                StepEnsureLatestFirmware(deps.flowUi, deps.firmwareUpdateManager),
                StepFetchDeviceId(),
                StepGetAPINetworks(deps.cloud),
                StepCheckIfTargetDeviceShouldBeClaimed(deps.cloud, deps.flowUi),
                StepEnsureTargetDeviceIsNotOnMeshNetwork(deps.cloud, deps.dialogTool),
                StepSetClaimCode(),
                StepShowTargetPairingSuccessful(deps.flowUi),
                StepDetermineFlowAfterPreflow(deps.flowUi)
            )


            JOINER_FLOW -> listOf(
                StepCollectMeshNetworkToJoinSelection(deps.flowUi),
                StepCollectCommissionerDeviceInfo(deps.flowUi),
                StepEnsureCommissionerConnected(deps.flowUi, deps.deviceConnector),
                StepEnsureCommissionerNetworkMatches(deps.flowUi, deps.cloud),
                StepCollectMeshNetworkToJoinPassword(deps.flowUi),
                StepShowJoiningMeshNetworkUi(deps.flowUi),
                StepJoinSelectedNetwork(deps.cloud),
                StepSetSetupDone(),
                StepEnsureListeningStoppedForBothDevices(),
                StepEnsureConnectionToCloud(),
                StepCheckDeviceGotClaimed(deps.cloud),
                StepSetNewDeviceName(deps.flowUi, deps.cloud),
                StepPublishDeviceSetupDoneEvent(deps.cloud),
                StepShowJoinerSetupFinishedUi(deps.flowUi)
            )


            INTERNET_CONNECTED_PREFLOW -> listOf(
                StepAwaitSetupStandAloneOrWithNetwork(deps.cloud, deps.flowUi)
            )


            ETHERNET_FLOW -> listOf(
                StepShowPricingImpact(deps.flowUi, deps.cloud),
                StepShowConnectingToDeviceCloudUi(deps.flowUi),
                StepSetSetupDone(),
                StepEnsureListeningStoppedForBothDevices(),
                StepEnsureEthernetHasIpAddress(deps.flowUi),
                StepEnsureConnectionToCloud(),
                StepCheckDeviceGotClaimed(deps.cloud),
                StepPublishDeviceSetupDoneEvent(deps.cloud)
            )


            WIFI_FLOW -> listOf(
                StepShowPricingImpact(deps.flowUi, deps.cloud),
                StepShowShouldConnectToDeviceCloudConfirmation(deps.flowUi),
                StepCollectUserWifiNetworkSelection(deps.flowUi),
                StepCollectSelectedWifiNetworkPassword(deps.flowUi),
                StepEnsureSelectedWifiNetworkJoined(deps.flowUi),
                // FIXME: this last sequence is virtually the same across setup flows -- unify them.
                StepSetSetupDone(),
                StepEnsureListeningStoppedForBothDevices(),
                StepShowWifiConnectingToDeviceCloudUi(deps.flowUi),
                StepEnsureConnectionToCloud(),
                StepCheckDeviceGotClaimed(deps.cloud),
                StepShowConnectedToCloudSuccessUi(deps.flowUi),
                StepPublishDeviceSetupDoneEvent(deps.cloud)
            )


            CELLULAR_FLOW -> listOf(
                StepEnsureCardOnFile(deps.flowUi, deps.cloud),
                StepFetchIccid(),
                StepEnsureSimActivationStatusUpdated(deps.cloud),
                StepShowPricingImpact(deps.flowUi, deps.cloud),
                StepShowShouldConnectToDeviceCloudConfirmation(deps.flowUi),
                StepShowCellularConnectingToDeviceCloudUi(deps.flowUi),
                StepEnsureSimActivated(deps.cloud),
                StepSetSetupDone(),
                StepEnsureListeningStoppedForBothDevices(),
                StepEnsureConnectionToCloud(),
                StepCheckDeviceGotClaimed(deps.cloud),
                StepShowConnectedToCloudSuccessUi(deps.flowUi),
                StepPublishDeviceSetupDoneEvent(deps.cloud)
            )


            NETWORK_CREATOR_POSTFLOW -> listOf(
                StepSetNewDeviceName(deps.flowUi, deps.cloud),
                StepGetNewMeshNetworkName(deps.flowUi),
                StepGetNewMeshNetworkPassword(deps.flowUi),
                StepShowCreateNewMeshNetworkUi(deps.flowUi),
                StepCreateNewMeshNetworkOnCloud(deps.cloud),
                StepCreateNewMeshNetworkOnLocalDevice(),
                StepEnsureConnectionToCloud(),
                StepShowCreateNetworkFinished(deps.flowUi)
            )

            STANDALONE_POSTFLOW -> listOf(
                StepSetNewDeviceName(deps.flowUi, deps.cloud),
                StepShowLetsGetBuildingUi(deps.flowUi)
            )

            CONTROL_PANEL_WIFI_INSPECT_NETWORK_FLOW -> listOf(
                StepEnsureListeningStoppedForBothDevices(),
                StepInspectCurrentWifiNetwork(deps.flowUi)
            )


            CONTROL_PANEL_CELLULAR_PRESENT_OPTIONS_FLOW -> listOf(
                StepFetchIccidFromCloud(deps.cloud, deps.flowUi),
                StepFetchFullSimData(deps.cloud, deps.flowUi),
                StepShowCellularOptionsUi(deps.flowUi)
            )


            CONTROL_PANEL_CELLULAR_SIM_DEACTIVATE -> listOf(
                StepShowSimDeactivateUi(deps.flowUi),
                StepDeactivateSim(deps.cloud, deps.flowUi)
            )


            CONTROL_PANEL_CELLULAR_SIM_REACTIVATE -> listOf(
                StepShowSimReactivateUi(deps.flowUi),
                StepReactivateSim(deps.cloud, deps.flowUi)
            )


            CONTROL_PANEL_CELLULAR_SIM_UNPAUSE -> listOf(
                StepShowSimUnpauseUi(deps.flowUi),
                StepUnpauseSim(deps.cloud, deps.flowUi)
            )

            CONTROL_PANEL_CELLULAR_SET_NEW_DATA_LIMIT -> listOf(
                StepShowSetDataLimitUi(deps.flowUi),
                StepSetDataLimit(deps.flowUi, deps.cloud),
                StepUnsetFullSimData(),
                StepFetchFullSimData(deps.cloud, deps.flowUi),
                StepPopBackStack(deps.flowUi)
            )

            CONTROL_PANEL_CELLULAR_SIM_ACTION_POSTFLOW -> listOf(
                StepUnsetFullSimData(),
                StepFetchFullSimData(deps.cloud, deps.flowUi),
                StepShowSnackbar(deps.flowUi),
                StepPopBackStack(deps.flowUi, true)
            )


            CONTROL_PANEL_MESH_INSPECT_NETWORK_FLOW -> listOf(
                StepFetchCurrentMeshNetwork(deps.flowUi),
                StepShowMeshInspectNetworkUi(deps.flowUi)
            )

            CONTROL_PANEL_MESH_LEAVE_NETWORK_FLOW -> listOf(
//                StepLeaveMeshNetwork(deps.cloud, deps.flowUi),
                StepPopBackStack(deps.flowUi)
            )


            SINGLE_TASK_POSTFLOW -> listOf(
                StepShowSingleTaskCongratsScreen(deps.flowUi)
            )
        }
    }

}