package io.particle.mesh.setup.ui


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.VideoView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import io.particle.common.buildRawResourceUri
import io.particle.sdk.app.BuildConfig
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_manual_commissioning_add_to_network.view.*


class ManualCommissioningAddToNetworkFragment : androidx.fragment.app.Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_manual_commissioning_add_to_network, container, false)

        root.action_next.setOnClickListener {
            findNavController().navigate(
                    R.id.action_manualCommissioningAddToNetworkFragment_to_scanCommissionerCodeFragment
            )
        }

        setUpVideoView(root.videoView)

        return root
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
