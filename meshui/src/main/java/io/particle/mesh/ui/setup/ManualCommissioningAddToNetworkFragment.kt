package io.particle.mesh.ui.setup


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.VideoView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import com.squareup.phrase.Phrase
import io.particle.android.common.buildRawResourceUri
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.R
import kotlinx.android.synthetic.main.fragment_manual_commissioning_add_to_network.*


class ManualCommissioningAddToNetworkFragment : BaseFlowFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_manual_commissioning_add_to_network,
            container,
            false
        )
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        action_next.setOnClickListener {
            // FIXME: this flow logic should live outside the UI
            findNavController().navigate(
                R.id.action_manualCommissioningAddToNetworkFragment_to_scanCommissionerCodeFragment
            )
        }

        setup_header_text.text = Phrase.from(view, R.string.add_xenon_to_mesh_network)
            .put("product_type", getUserFacingTypeName())
            .format()

        setUpVideoView(videoView)
    }

    private fun setUpVideoView(vidView: VideoView) {
        vidView.setVideoURI(activity!!.buildRawResourceUri(R.raw.commissioner_to_listening_mode))

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
