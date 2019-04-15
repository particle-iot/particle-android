package io.particle.android.sdk.controlpanel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_cp_congrats.*
import kotlinx.coroutines.delay
import mu.KotlinLogging


class ControlPanelCongratsFragment : BaseControlPanelFragment() {

    private val log = KotlinLogging.logger {}

    override val titleBarOptions = TitleBarOptions()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_cp_congrats, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        p_hashtagwinning_message.text = responseReceiver?.singleTaskCongratsMessage

        if (!meshModel.scopes.job.isCancelled) {
            meshModel.scopes.onMain {
                delay(2000)
                if (!isDetached && isAdded) {
                    findNavController().popBackStack()
                }
            }
        }
    }

}