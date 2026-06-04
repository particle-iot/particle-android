package io.particle.mesh.ui.setup


import android.media.AudioManager
import android.os.Build
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
import io.particle.mesh.common.QATool
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.R
import io.particle.mesh.ui.databinding.FragmentManualCommissioningAddToNetworkBinding


class ManualCommissioningAddToNetworkFragment : BaseFlowFragment() {

    private var _binding: FragmentManualCommissioningAddToNetworkBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentManualCommissioningAddToNetworkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        binding.actionNext.setOnClickListener {
            // FIXME: this flow logic should live outside the UI
            try {
                findNavController().navigate(
                    R.id.action_manualCommissioningAddToNetworkFragment_to_scanCommissionerCodeFragment
                )
            } catch (ex: Exception) {
                // Workaround to avoid this seemingly impossible crash: http://bit.ly/2kTpnIb
                val error = IllegalStateException(
                    "Navigation error, Activity=${activity.javaClass}, isFinishing=${activity.isFinishing}",
                    ex
                )
                QATool.report(error)
                this@ManualCommissioningAddToNetworkFragment.activity?.finish()
            }
        }

        binding.setupHeaderText.text = Phrase.from(view, R.string.add_xenon_to_mesh_network)
            .put("product_type", getUserFacingTypeName())
            .format()

        setUpVideoView(binding.videoView)
    }

    private fun setUpVideoView(vidView: VideoView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // stop pausing the user's music when showing the video!
            vidView.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
        }

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
