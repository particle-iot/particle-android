package io.particle.mesh.ui.controlpanel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.VideoView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.particle.android.common.buildRawResourceUri
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import kotlinx.android.synthetic.main.fragment_cp_prepare_for_pairing.*
import mu.KotlinLogging


class PrepareForPairingFragment : BaseControlPanelFragment() {

    private val log = KotlinLogging.logger {}

    override var titleBarOptions = TitleBarOptions(R.string.p_controlpanel_prepare_for_pairing)

    private val cloud: ParticleCloud = ParticleCloudSDK.getCloud()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_cp_prepare_for_pairing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpVideoView(videoView)

        p_controlpanel_signal_switch.setOnCheckedChangeListener { _, isChecked ->
            onSignalSwitchChanged(isChecked)
        }
    }

    override fun onStop() {
        super.onStop()
        onSignalSwitchChanged(false)
    }

    private fun onSignalSwitchChanged(isChecked: Boolean) {
        val deviceId = (activity as ControlPanelActivity).deviceId
        flowScopes.onWorker {
            val device = cloud.getDevice(deviceId)
            try {
                device.startStopSignaling(isChecked)
            } catch (ex: Exception) {
                log.error(ex) { "Error turning rainbow-shouting ${if (isChecked) "ON" else "OFF"}" }
            }
        }
    }

    private fun setUpVideoView(vidView: VideoView) {
        vidView.setVideoURI(requireActivity().buildRawResourceUri(R.raw.commissioner_to_listening_mode))

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                vidView.start()
            }

            override fun onStop(owner: LifecycleOwner) {
                vidView.stopPlayback()
            }
        })

        vidView.setOnPreparedListener { player -> player.isLooping = true }
    }

}