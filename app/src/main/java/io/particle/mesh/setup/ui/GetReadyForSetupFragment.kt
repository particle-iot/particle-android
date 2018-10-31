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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import com.squareup.phrase.Phrase
import io.particle.common.buildRawResourceUri
import io.particle.mesh.setup.flow.MeshDeviceType
import io.particle.sdk.app.R
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // stop pausing the user's music when showing the video!
            videoView.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
        }
        setContentFromDeviceModel()

        setUpVideoView(videoView)
    }

    private fun setContentFromDeviceModel() {
        if (p_getreadyforsetup_use_ethernet_switch.isChecked) {
            onConfigChanged(HelpTextConfig.FEATHERWING)
            return
        }

        val config = when (flowManagerVM.flowManager!!.targetDeviceType) {
            MeshDeviceType.ARGON -> HelpTextConfig.ARGON
            MeshDeviceType.XENON -> HelpTextConfig.XENON
            MeshDeviceType.BORON -> HelpTextConfig.BORON_3G
        }
        onConfigChanged(config)
    }

    private fun onNext() {
        flowManagerVM.flowManager!!.deviceModule.updateShouldDetectEthernet(
            p_getreadyforsetup_use_ethernet_switch.isChecked
        )
        findNavController().navigate(R.id.action_getReadyForSetupFragment_to_scanCodeIntroFragment)
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
    )

}
