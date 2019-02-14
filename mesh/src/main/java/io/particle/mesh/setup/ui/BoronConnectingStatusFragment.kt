package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.ui.utils.markProgress
import io.particle.mesh.R


class BoronConnectingStatusFragment : BaseMeshSetupFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_boron_connecting_status, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val cloudModule = flowManagerVM.flowManager!!.cloudConnectionModule

        markProgress(true, R.id.status_stage_1)

        cloudModule.boronSteps.isSimActivatedLD.observe(this, Observer {
            if (it.truthy()) {
                markProgress(true, R.id.status_stage_2)
            }
        })

        cloudModule.targetDeviceConnectedToCloud.observe(this, Observer {
            if (it.truthy()) {
                markProgress(true, R.id.status_stage_3)
            }
        })
    }

}
