package io.particle.mesh.ui.controlpanel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.android.sdk.cloud.ParticleNetwork
import io.particle.firmwareprotos.ctrl.mesh.Mesh
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.setup.flow.DialogResult.NEGATIVE
import io.particle.mesh.setup.flow.DialogResult.POSITIVE
import io.particle.mesh.setup.flow.DialogSpec.StringDialogSpec
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import io.particle.mesh.ui.databinding.FragmentControlpanelMeshNetworkInfoBinding
import mu.KotlinLogging


class ControlPanelMeshInspectNetworkFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(
        R.string.p_controlpanel_mesh_inspect_title,
        showBackButton = true
    )

    private val log = KotlinLogging.logger {}

    private val cloud = ParticleCloudSDK.getCloud()

    private var cachedMeshNetworkDataFromDevice: Mesh.NetworkInfo? = null
    private var cachedMeshNetworkDataFromCloud: ParticleNetwork? = null

    private var _binding: FragmentControlpanelMeshNetworkInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentControlpanelMeshNetworkInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)
        log.info { "onFragmentReady()" }

        binding.pControlpanelActionLeaveNetwork.setOnClickListener { leaveNetwork() }

        updateLocalNetworkInfoCaches(flowUiListener)

        onNetworkInfoUpdated(cachedMeshNetworkDataFromDevice!!)
    }

    private fun updateLocalNetworkInfoCaches(flowUiListener: FlowRunnerUiListener) {
        cachedMeshNetworkDataFromDevice = flowUiListener.mesh.currentlyJoinedNetwork!!
        cachedMeshNetworkDataFromCloud = flowUiListener.cloud.meshNetworksFromAPI!!
            .firstOrNull { cachedMeshNetworkDataFromDevice?.networkId == it.id }
    }

    override fun onResume() {
        super.onResume()
        onNetworkInfoUpdated(cachedMeshNetworkDataFromDevice!!)
    }

    private fun onNetworkInfoUpdated(networkInfo: Mesh.NetworkInfo) {
        log.info { "onNetworkInfoUpdated(): $networkInfo" }

        binding.pControlpanelMeshInspectNetworkName.text = networkInfo.name
        binding.pControlpanelMeshInspectNetworkPanId.text = networkInfo.panId.toString()
        binding.pControlpanelMeshInspectNetworkXpanId.text = networkInfo.extPanId.toString()
        binding.pControlpanelMeshInspectNetworkChannel.text = networkInfo.channel.toString()
        binding.pControlpanelMeshInspectNetworkNetworkId.text = networkInfo.networkId

        flowScopes.onMain {
            flowSystemInterface.showGlobalProgressSpinner(true)

            val (isGateway, count) = flowScopes.withWorker {
                val meshMemberships = cloud.getNetworkDevices(networkInfo.networkId)
                val isGw = meshMemberships.firstOrNull { it.deviceId == device.id }
                    ?.membership
                    ?.roleData
                    ?.isGateway
                return@withWorker Pair(isGw, meshMemberships.size)
            }

            val roleLabel = if (isGateway == true) "Gateway" else "Node"
            binding.pControlpanelMeshInspectNetworkDeviceRole.text = roleLabel
            binding.pControlpanelMeshInspectNetworkDeviceCount.text = count.toString()

            flowSystemInterface.showGlobalProgressSpinner(false)
        }
    }

    private fun leaveNetwork() {
        val meshName = cachedMeshNetworkDataFromDevice?.name

        flowScopes.onMain {
            val result = flowSystemInterface.dialogHack.dialogResultLD
                .nonNull(flowScopes)
                .runBlockOnUiThreadAndAwaitUpdate(flowScopes) {
                    flowSystemInterface.dialogHack.newDialogRequest(
                        StringDialogSpec(
                            "Remove this device from the current mesh network, '$meshName'?",
                            "Leave network",
                            "Cancel",
                            "Leave current network?"
                        )
                    )
                }

            when (result) {
                POSITIVE -> {
                    startFlowWithBarcode(
                        flowRunner::startControlPanelMeshLeaveCurrentMeshNetworkFlow
                    )
                }
                else -> { /* no-op */
                }
            }
        }
    }
}
