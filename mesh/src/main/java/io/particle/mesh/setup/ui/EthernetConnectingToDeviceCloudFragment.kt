package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.squareup.phrase.Phrase
import io.particle.mesh.setup.ui.utils.markProgress
import io.particle.mesh.R
import kotlinx.android.synthetic.main.fragment_ethernet_connecting_to_device_cloud.*


class EthernetConnectingToDeviceCloudFragment : BaseMeshSetupFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ethernet_connecting_to_device_cloud, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // we're doing this from the outset, so mark it checked already
        markProgress(true, R.id.status_stage_1)

        val fm = flowManagerVM.flowManager!!
        fm.cloudConnectionModule.targetDeviceConnectedToCloud.observeForProgress(R.id.status_stage_2)
        fm.cloudConnectionModule.targetOwnedByUserLD.observeForProgress(R.id.status_stage_3)

        setup_header_text.text = Phrase.from(view, R.string.p_connectingtodevicecloud_title)
            .put("product_type", fm.getTypeName())
            .format()
    }

    private fun LiveData<Boolean?>.observeForProgress(@IdRes progressStage: Int) {
        this.observe(
            this@EthernetConnectingToDeviceCloudFragment,
            Observer { markProgress(it, progressStage) }
        )
    }
}
