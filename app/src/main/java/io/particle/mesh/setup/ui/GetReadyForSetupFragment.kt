package io.particle.mesh.setup.ui


import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.VideoView
import androidx.navigation.Navigation
import com.squareup.phrase.Phrase
import io.particle.common.buildRawResourceUri
import io.particle.sdk.app.BuildConfig
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_get_ready_for_setup.view.*

class GetReadyForSetupFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_get_ready_for_setup, container, false)

        setUpVideoView(root.videoView)
        root.action_next.setOnClickListener(Navigation.createNavigateOnClickListener(
                R.id.action_getReadyForSetupFragment_to_scanCodeIntroFragment
//                R.id.action_global_newMeshNetworkPasswordFragment
        ))

        // FIXME: when "use ethernet" is toggled, switch to R.raw.featherwing_power video

        val productName = flowManagerVM.flowManager!!.getTypeName(root.context)

        root.setup_header_text.text = Phrase.from(root, R.string.get_your_xenon_ready_for_setup)
                .put("product_type", productName)
                .format()

        root.textView.text = Phrase.from(root, R.string.plug_your_device_into_a_power_source)
                .put("product_type", productName)
                .format()

        root.textView3.text = Phrase.from(root, R.string.confirm_your_device_is_blinking_blue)
                .put("product_type", productName)
                .format()

        return root
    }

    private fun setUpVideoView(vidView: VideoView) {
        vidView.setVideoURI(requireActivity().buildRawResourceUri(R.raw.power_on_xenon_with_breadboard))

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
