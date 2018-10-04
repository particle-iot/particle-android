package io.particle.mesh.setup.ui


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.VideoView
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.Navigation
import com.squareup.phrase.Phrase
import io.particle.common.buildRawResourceUri
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_get_ready_for_setup.*
import java.lang.Exception


class GetReadyForSetupFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_get_ready_for_setup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        action_next.setOnClickListener(Navigation.createNavigateOnClickListener(
                R.id.action_getReadyForSetupFragment_to_scanCodeIntroFragment
        ))

        p_getreadyforsetup_use_ethernet_switch.setOnCheckedChangeListener { _, isChecked ->
            val config = if (isChecked) HelpTextConfig.ETHERNET else HelpTextConfig.MESH_ONLY
            onEthernetSwitched(config)
        }

        onEthernetSwitched(HelpTextConfig.MESH_ONLY)

        setUpVideoView(videoView)
    }

    private fun onEthernetSwitched(config: HelpTextConfig) {
        val productName = flowManagerVM.flowManager!!.getTypeName(requireContext())

        setup_header_text.setTextMaybeWithProductTypeFormat(productName, config.headerText)
        videoView.setVideoURI(requireActivity().buildRawResourceUri(config.videoUrlRes))
        val textViews = mapOf(
                p_mesh_step1 to config.step1,
                p_mesh_step2 to config.step2,
                p_mesh_step3 to config.step3,
                p_mesh_step4 to config.step4
        )
        for ((view, res) in textViews.entries) {
            if (res == null) {
                view.visibility = View.INVISIBLE
            } else {
                view.visibility = View.VISIBLE
                view.setTextMaybeWithProductTypeFormat(productName, res)
            }
        }
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
        @StringRes val step1: Int,
        @StringRes val step2: Int,
        @StringRes val step3: Int?,
        @StringRes val step4: Int?,
        @RawRes val videoUrlRes: Int
) {

    MESH_ONLY(
            R.string.p_getreadyforsetup_header_mesh_only,
            R.string.p_getreadyforsetup_step1_mesh_only,
            R.string.p_getreadyforsetup_step2_mesh_only,
            null,
            null,
            R.raw.power_on_xenon_with_breadboard
    ),

    ETHERNET(
            R.string.p_getreadyforsetup_header_ethernet,
            R.string.p_getreadyforsetup_step1_ethernet,
            R.string.p_getreadyforsetup_step2_ethernet,
            R.string.p_getreadyforsetup_step3_ethernet,
            R.string.p_getreadyforsetup_step4_ethernet,
            R.raw.featherwing_power
    )
}
