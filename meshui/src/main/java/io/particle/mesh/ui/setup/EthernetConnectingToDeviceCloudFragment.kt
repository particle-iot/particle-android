package io.particle.mesh.ui.setup


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.squareup.phrase.Phrase
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.utils.markProgress
import io.particle.mesh.ui.R
import kotlinx.android.synthetic.main.fragment_ethernet_connecting_to_device_cloud.*


class EthernetConnectingToDeviceCloudFragment : BaseFlowFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ethernet_connecting_to_device_cloud, container, false)
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        // we're doing this from the outset, so mark it checked already
        markProgress(true, R.id.status_stage_1)

        val target = flowUiListener.targetDevice
        target.isDeviceConnectedToCloudLD.observeForProgress(R.id.status_stage_2)
        target.isClaimedLD.observeForProgress(R.id.status_stage_3)

        setup_header_text.text = Phrase.from(view, R.string.p_connectingtodevicecloud_title)
            .put("product_type", getUserFacingTypeName())
            .format()
    }

    private fun LiveData<Boolean?>.observeForProgress(@IdRes progressStage: Int) {
        this.observe(
            this@EthernetConnectingToDeviceCloudFragment,
            Observer { markProgress(it, progressStage) }
        )
    }
}
