package io.particle.mesh.ui.setup


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.utils.markProgress
import io.particle.mesh.ui.R


class BoronConnectingStatusFragment : BaseFlowFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_boron_connecting_status, container, false)
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        markProgress(true, R.id.status_stage_1)

        flowUiListener.targetDevice.isSimActivatedLD.observe(this, Observer {
            if (it.truthy()) {
                markProgress(true, R.id.status_stage_2)
            }
        })

        flowUiListener.targetDevice.isDeviceConnectedToCloudLD.observe(this, Observer {
            if (it.truthy()) {
                markProgress(true, R.id.status_stage_3)
            }
        })
    }

}
