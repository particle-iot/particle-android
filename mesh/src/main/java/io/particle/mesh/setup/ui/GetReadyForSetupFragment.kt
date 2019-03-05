package io.particle.mesh.setup.ui


import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.VideoView
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.squareup.phrase.Phrase
import io.particle.android.common.buildRawResourceUri
import io.particle.mesh.R
import io.particle.mesh.setup.flow.Gen3ConnectivityType
import io.particle.mesh.setup.isSomSerial
import io.particle.mesh.setup.ui.HelpTextConfig.ARGON
import io.particle.mesh.setup.ui.HelpTextConfig.A_SERIES
import io.particle.mesh.setup.ui.HelpTextConfig.BORON_3G
import io.particle.mesh.setup.ui.HelpTextConfig.BORON_LTE
import io.particle.mesh.setup.ui.HelpTextConfig.B_SERIES
import io.particle.mesh.setup.ui.HelpTextConfig.FEATHERWING
import io.particle.mesh.setup.ui.HelpTextConfig.XENON
import io.particle.mesh.setup.ui.HelpTextConfig.X_SERIES
import kotlinx.android.synthetic.main.fragment_get_ready_for_setup.*


class GetReadyForSetupFragment : BaseMeshSetupFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_get_ready_for_setup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        action_next.setOnClickListener { onNext() }

        p_getreadyforsetup_use_ethernet_switch.setOnCheckedChangeListener { _, _ ->
            setContentFromDeviceModel()
        }

        p_getreadyforsetup_antenna_confirmation_speedbump.setOnCheckedChangeListener { _, isChecked ->
            action_next.isEnabled = isChecked
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // stop pausing the user's music when showing the video!
            videoView.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
        }
        setContentFromDeviceModel()

        setUpVideoView(videoView)
    }

    private fun setContentFromDeviceModel() {
        val config = if (p_getreadyforsetup_use_ethernet_switch.isChecked) {
            HelpTextConfig.FEATHERWING
        } else {
            val barcodeLD = flowManagerVM.flowManager!!.bleConnectionModule.targetDeviceBarcodeLD
            val isSomSerial = barcodeLD.value?.serialNumber?.isSomSerial() ?: false

            when (flowManagerVM.flowManager!!.targetDeviceType) {
                Gen3ConnectivityType.WIFI -> { if (isSomSerial) HelpTextConfig.A_SERIES else HelpTextConfig.ARGON }
                Gen3ConnectivityType.MESH_ONLY -> { if (isSomSerial) HelpTextConfig.X_SERIES else HelpTextConfig.XENON }
                Gen3ConnectivityType.CELLULAR -> { if (isSomSerial) HelpTextConfig.B_SERIES else HelpTextConfig.BORON_3G }
            }
        }

        p_getreadyforsetup_antenna_confirmation_speedbump.isVisible = when (config) {
            FEATHERWING,
            XENON -> {
                action_next.isEnabled = true
                false
            }
            ARGON,
            BORON_LTE,
            BORON_3G,
            A_SERIES,
            B_SERIES,
            X_SERIES -> {
                action_next.isEnabled = p_getreadyforsetup_antenna_confirmation_speedbump.isChecked
                true
            }
        }

        onConfigChanged(config)
    }

    private fun onNext() {
        flowManagerVM.flowManager!!.deviceModule.updateShouldDetectEthernet(
            p_getreadyforsetup_use_ethernet_switch.isChecked
        )

        flowManagerVM.flowManager!!.bleConnectionModule.updateGetReadyNextButtonClicked(true)
    }

    private fun onConfigChanged(config: HelpTextConfig) {
        val productName = flowManagerVM.flowManager!!.getTypeName()

        setup_header_text.setTextMaybeWithProductTypeFormat(productName, config.headerText)
        videoView.setVideoURI(requireActivity().buildRawResourceUri(config.videoUrlRes))
    }

    private fun setUpVideoView(vidView: VideoView) {
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

private fun TextView.setTextMaybeWithProductTypeFormat(
    productName: String,
    @StringRes strId: Int
) {
    this.text = try {
        Phrase.from(this, strId)
            .put("product_type", productName)
            .format()
    } catch (ex: Exception) {
        this.context.getString(strId)
    }
}


internal enum class HelpTextConfig(
    @StringRes val headerText: Int,
    @RawRes val videoUrlRes: Int
) {

    XENON(
        R.string.p_getreadyforsetup_header_text,
        R.raw.power_on_xenon
    ),

    FEATHERWING(
        R.string.p_getreadyforsetup_header_ethernet,
        R.raw.power_on_featherwing
    ),

    ARGON(
        R.string.p_getreadyforsetup_header_text,
        R.raw.power_on_argon
    ),

    BORON_LTE(
        R.string.p_getreadyforsetup_header_text,
//        R.raw.power_on_boron
        R.raw.power_on_boron_battery
    ),

    BORON_3G(
        R.string.p_getreadyforsetup_header_text,
        R.raw.power_on_boron_battery
    ),

    A_SERIES(
        R.string.p_getreadyforsetup_header_text,
        R.raw.power_on_argon
    ),

    B_SERIES(
        R.string.p_getreadyforsetup_header_text,
        R.raw.power_on_boron_battery
    ),

    X_SERIES(
        R.string.p_getreadyforsetup_header_text,
        R.raw.power_on_xenon
    )

}
