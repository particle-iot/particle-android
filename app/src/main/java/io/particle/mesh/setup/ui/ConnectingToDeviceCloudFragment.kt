package io.particle.mesh.setup.ui


import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.annotation.IdRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.particle.mesh.common.truthy
import io.particle.sdk.app.R
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch


class ConnectingToDeviceCloudFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_connecting_to_device_cloud, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // we're doing this from the outset, so mark it checked already
        markProgress(true, R.id.status_stage_1)

        val fm = flowManagerVM.flowManager!!
        fm.cloudConnectionModule.targetDeviceConnectedToCloud.observeForProgress(R.id.status_stage_2)
        fm.cloudConnectionModule.targetOwnedByUserLD.observeForProgress(R.id.status_stage_3)
    }

    private fun LiveData<Boolean?>.observeForProgress(@IdRes progressStage: Int) {
        this.observe(
                this@ConnectingToDeviceCloudFragment,
                Observer { markProgress(it, progressStage) }
        )
    }

    private fun markProgress(update: Boolean?, @IdRes progressStage: Int) {
        if (!update.truthy()) {
            return
        }
        launch(UI) {
            val tv: TextView? = view?.findViewById(progressStage)
            tv?.text = "✓ " + tv?.text
        }
    }
}
