package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.particle.mesh.R
import kotlinx.android.synthetic.main.fragment_hashtag_winning.*


class HashtagWinningFragment : BaseMeshSetupFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_hashtag_winning, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = HashtagWinningFragmentArgs.fromBundle(this.arguments!!)
        p_hashtagwinning_message.text = args.congratsMessage
    }

}
