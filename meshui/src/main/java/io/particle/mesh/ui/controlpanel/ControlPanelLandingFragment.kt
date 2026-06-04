package io.particle.mesh.ui.controlpanel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import io.particle.android.sdk.cloud.BroadcastContract
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ARGON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.A_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.B5_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.BLUZ
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.BORON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.B_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.CORE
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.DIGISTUMP_OAK
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ELECTRON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ESP32
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.OTHER
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.P1
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.PHOTON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.RASPBERRY_PI
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.RED_BEAR_DUO
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.XENON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.X_SOM
import io.particle.commonui.DeviceNotesDelegate
import io.particle.commonui.RenameHelper
import io.particle.mesh.common.android.livedata.BroadcastReceiverLD
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import io.particle.mesh.ui.databinding.FragmentControlPanelLandingBinding
import mu.KotlinLogging


class ControlPanelLandingFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(R.string.p_controlpanel_control_panel)

    private lateinit var cloud: ParticleCloud

    private lateinit var devicesUpdatedBroadcast: BroadcastReceiverLD<Int>
    private val flowManagementScope = Scopes()

    private val log = KotlinLogging.logger {}

    private var _binding: FragmentControlPanelLandingBinding? = null
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cloud = ParticleCloudSDK.getCloud()

        var initialValue = 0
        devicesUpdatedBroadcast = BroadcastReceiverLD(
            requireActivity(),
            BroadcastContract.BROADCAST_DEVICES_UPDATED,
            { ++initialValue },
            true
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentControlPanelLandingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        devicesUpdatedBroadcast.observe(viewLifecycleOwner, Observer { updateDetails() })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)
        val deviceType = device.deviceType!!

        binding.pControlpanelLandingNameFrame.setOnClickListener {
            RenameHelper.renameDevice(activity, device)
        }

        binding.pControlpanelLandingNotesFrame.setOnClickListener { editNotes() }

        binding.networkInfoHeader.isVisible = deviceType in gen3Devices

        binding.pControlpanelLandingWifiItemFrame.isVisible = deviceType in listOf(ARGON, A_SOM)
        binding.pControlpanelLandingWifiItem.setOnClickListener {
            flowScopes.onMain {
                startFlowWithBarcode(flowRunner::startControlPanelInspectCurrentWifiNetworkFlow)
            }
        }

        binding.pControlpanelLandingCellularItemFrame.isVisible = deviceType in listOf(BORON, B_SOM, B5_SOM)
        binding.pControlpanelLandingCellularItem.setOnClickListener {
            flowRunner.startShowControlPanelCellularOptionsFlow(device)
        }

        binding.pControlpanelLandingEthernetItemFrame.isVisible = deviceType in gen3Devices
        binding.pControlpanelLandingEthernetItemFrame.setOnClickListener {
            flowScopes.onMain {
                startFlowWithBarcode(flowRunner::startShowControlPanelEthernetOptionsFlow)
            }
        }

        binding.pControlpanelLandingMeshItem.isVisible = deviceType in gen3Devices
        binding.pControlpanelLandingMeshItem.setOnClickListener {
            val uri: Uri = Uri.parse(
                "https://docs.particle.io/reference/developer-tools/cli/#particle-mesh"
            )
            val intent = Intent(Intent.ACTION_VIEW, uri)
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            }
        }

        binding.pControlpanelLandingDocsItem.setOnClickListener {
            showDocumentation(activity, device.deviceType!!)
        }

        binding.pControlpanelLandingUnclaimItem.setOnClickListener {
            navigateToUnclaim()
        }
    }

    override fun onResume() {
        super.onResume()
        updateDetails()
    }

    override fun onStop() {
        super.onStop()
        log.info { "onStop()" }
    }

    private fun updateDetails() {
        binding.pControlpanelLandingNameValue.text = device.name
        binding.pControlpanelLandingNotesValue.text = device.notes
    }

    private fun navigateToUnclaim() {
        flowSystemInterface.showGlobalProgressSpinner(true)
        findNavController().navigate(
            R.id.action_global_controlPanelUnclaimDeviceFragment,
            ControlPanelUnclaimDeviceFragmentArgs(device.name).toBundle()
        )
        flowSystemInterface.showGlobalProgressSpinner(false)
    }

    private fun editNotes() {
        val editLD = MutableLiveData<String>()
        DeviceNotesDelegate.editDeviceNotes(
            requireActivity(),
            device,
            flowManagementScope,
            editLD
        )
        editLD.observe(this, Observer {
            binding.pControlpanelLandingNotesValue.text = it
        })
    }
}

private val gen3Devices = setOf(
    ParticleDeviceType.ARGON,
    ParticleDeviceType.A_SOM,
    ParticleDeviceType.BORON,
    ParticleDeviceType.B_SOM,
    ParticleDeviceType.B5_SOM,
    ParticleDeviceType.XENON,
    ParticleDeviceType.X_SOM
)


private fun showDocumentation(context: Context, deviceType: ParticleDeviceType) {
    val finalPathSegment = when (deviceType) {
        CORE -> "core"
        PHOTON -> "photon"
        P1 -> "datasheets/wi-fi/p1-datasheet"
        ELECTRON -> "electron"
        ARGON, A_SOM -> "argon"
        BORON, B_SOM, B5_SOM -> "boron"
        XENON, X_SOM -> "xenon"
        RASPBERRY_PI,
        RED_BEAR_DUO,
        BLUZ,
        DIGISTUMP_OAK,
        ESP32,
        OTHER -> null
    }

    finalPathSegment?.let {
        val uri = Uri.parse("https://docs.particle.io/$finalPathSegment")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}
