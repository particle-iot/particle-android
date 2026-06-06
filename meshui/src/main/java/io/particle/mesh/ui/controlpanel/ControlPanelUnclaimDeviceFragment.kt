package io.particle.mesh.ui.controlpanel


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.WorkerThread
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.navArgs
import com.squareup.phrase.Phrase
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.setup.flow.FlowTerminationAction
import io.particle.mesh.setup.utils.ToastDuration
import io.particle.mesh.setup.utils.safeToast
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import io.particle.mesh.ui.databinding.FragmentControlPanelUnclaimDeviceBinding


class ControlPanelUnclaimDeviceFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(
        R.string.unclaim_title,
        showBackButton = true
    )

    private val args: ControlPanelUnclaimDeviceFragmentArgs by navArgs()
    private val cloud = ParticleCloudSDK.getCloud()

    private var _binding: FragmentControlPanelUnclaimDeviceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentControlPanelUnclaimDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        binding.unclaimBody.text = Phrase.from(binding.unclaimBody.text)
            .put("device_name", args.deviceName)
            .format()

        binding.actionUnclaimDevice.setOnClickListener {
            flowScopes.onWorker { unclaimDevice() }
        }
    }

    @WorkerThread
    private fun unclaimDevice() {
        flowSystemInterface.showGlobalProgressSpinner(true)

        val err = try {
            device.unclaim()
            null

        } catch (ex: Exception) {
            ex

        } finally {
            flowScopes.onMain {
                flowSystemInterface.showGlobalProgressSpinner(false)
            }
        }

        flowScopes.onMain {
            if (!isAdded) {
                return@onMain
            }

            activity?.safeToast(
                if (err == null) "Device unclaimed!" else "Error unclaiming device",
                ToastDuration.LONG
            )
            flowSystemInterface.terminateSetup(FlowTerminationAction.NoFurtherAction)
        }
    }
}
