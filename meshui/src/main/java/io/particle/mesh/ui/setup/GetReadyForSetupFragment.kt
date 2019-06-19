package io.particle.mesh.ui.setup


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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.squareup.phrase.Phrase
import io.particle.android.common.buildRawResourceUri
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.A_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.B_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.X_SOM
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.setup.flow.Gen3ConnectivityType
import io.particle.mesh.setup.isSomSerial
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.R
import io.particle.mesh.ui.setup.HelpTextConfig.ARGON
import io.particle.mesh.ui.setup.HelpTextConfig.A_SERIES
import io.particle.mesh.ui.setup.HelpTextConfig.A_SERIES_ETHERNET
import io.particle.mesh.ui.setup.HelpTextConfig.BORON_3G
import io.particle.mesh.ui.setup.HelpTextConfig.BORON_LTE
import io.particle.mesh.ui.setup.HelpTextConfig.B_SERIES
import io.particle.mesh.ui.setup.HelpTextConfig.B_SERIES_ETHERNET
import io.particle.mesh.ui.setup.HelpTextConfig.FEATHERWING
import io.particle.mesh.ui.setup.HelpTextConfig.XENON
import io.particle.mesh.ui.setup.HelpTextConfig.X_SERIES
import io.particle.mesh.ui.setup.HelpTextConfig.X_SERIES_ETHERNET
import kotlinx.android.synthetic.main.fragment_get_ready_for_setup.*


class GetReadyForSetupFragment : BaseFlowFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_get_ready_for_setup, container, false)
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

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
        val ful = flowUiListener!!

        val isSomSerial = ful.targetDevice.deviceType?.isSoM() ?: false

        val config = if (p_getreadyforsetup_use_ethernet_switch.isChecked) {
            when (ful.targetDevice.deviceType) {
                A_SOM -> A_SERIES_ETHERNET
                B_SOM -> B_SERIES_ETHERNET
                X_SOM -> X_SERIES_ETHERNET
                else -> FEATHERWING
            }
        } else {
            when (ful.targetDevice.connectivityType!!) {
                Gen3ConnectivityType.WIFI -> {
                    if (isSomSerial) A_SERIES else ARGON
                }
                Gen3ConnectivityType.MESH_ONLY -> {
                    if (isSomSerial) X_SERIES else XENON
                }
                Gen3ConnectivityType.CELLULAR -> {
                    if (isSomSerial) B_SERIES else BORON_3G
                }
            }
        }

        p_getreadyforsetup_antenna_confirmation_speedbump.isVisible = when (config) {
            FEATHERWING,
            A_SERIES_ETHERNET,
            B_SERIES_ETHERNET,
            X_SERIES_ETHERNET,
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
        val shouldDetect = p_getreadyforsetup_use_ethernet_switch.isChecked
        flowUiListener?.deviceData?.shouldDetectEthernet = shouldDetect
        flowUiListener?.onGetReadyNextButtonClicked()
    }

    private fun onConfigChanged(config: HelpTextConfig) {
        val productName = getUserFacingTypeName()

        setup_header_text.setTextMaybeWithProductTypeFormat(productName, config.headerText)
        videoView.setVideoURI(requireActivity().buildRawResourceUri(config.videoUrlRes))
        config.speedbumpCheckboxText?.let {
            p_getreadyforsetup_antenna_confirmation_speedbump.setText(it)
        }
        val label  = getString(config.ethernetSwitchLabel)
        p_getreadyforsetup_use_ethernet_switch.text = label
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
    @RawRes val videoUrlRes: Int,
    @StringRes val speedbumpCheckboxText: Int? = null,
    @StringRes val ethernetSwitchLabel: Int = R.string.p_getreadyforsetup_use_ethernet_switch
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
        R.raw.power_on_argon,
        R.string.p_getreadyforsetup_antenna_confirmation_speedbump_argon
    ),

    BORON_LTE(
        R.string.p_getreadyforsetup_header_text,
//        R.raw.power_on_boron
        R.raw.power_on_boron_battery,
        R.string.p_getreadyforsetup_antenna_confirmation_speedbump_boron
    ),

    BORON_3G(
        R.string.p_getreadyforsetup_header_text,
        R.raw.power_on_boron_battery,
        R.string.p_getreadyforsetup_antenna_confirmation_speedbump_boron
    ),

    A_SERIES(
        R.string.p_getreadyforsetup_header_text,
        R.raw.power_on_argon,
        R.string.p_getreadyforsetup_antenna_confirmation_speedbump_asom,
        R.string.p_getreadyforsetup_use_ethernet_switch_som
    ),

    B_SERIES(
        R.string.p_getreadyforsetup_header_text,
        R.raw.power_on_boron_battery,
        R.string.p_getreadyforsetup_antenna_confirmation_speedbump_bsom,
        R.string.p_getreadyforsetup_use_ethernet_switch_som
    ),

    X_SERIES(
        R.string.p_getreadyforsetup_header_text,
        R.raw.power_on_xenon,
        R.string.p_getreadyforsetup_antenna_confirmation_speedbump_xsom,
        R.string.p_getreadyforsetup_use_ethernet_switch_som
    ),

    A_SERIES_ETHERNET(
        R.string.p_getreadyforsetup_header_text,
        R.raw.power_on_argon,
        null,
        R.string.p_getreadyforsetup_use_ethernet_switch_som
    ),

    B_SERIES_ETHERNET(
        R.string.p_getreadyforsetup_header_text,
        R.raw.power_on_boron_battery,
        null,
        R.string.p_getreadyforsetup_use_ethernet_switch_som
    ),

    X_SERIES_ETHERNET(
        R.string.p_getreadyforsetup_header_text,
        R.raw.power_on_xenon,
        null,
        R.string.p_getreadyforsetup_use_ethernet_switch_som
    )

}


private fun ParticleDeviceType.isSoM(): Boolean {
    return when(this) {
        A_SOM,
        B_SOM,
        X_SOM -> true
        else -> false
    }
}