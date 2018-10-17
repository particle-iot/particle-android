package io.particle.mesh.setup.ui


import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.squareup.phrase.Phrase
import io.particle.mesh.setup.ui.utils.markProgress
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_connecting_to_device_cloud.*


class ConnectingToDeviceCloudFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_connecting_to_device_cloud, container, false)
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
                this@ConnectingToDeviceCloudFragment,
                Observer { markProgress(it, progressStage) }
        )
    }
}
