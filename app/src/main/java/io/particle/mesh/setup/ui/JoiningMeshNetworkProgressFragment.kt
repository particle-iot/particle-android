package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.particle.mesh.setup.ui.utils.markProgress
import io.particle.sdk.app.R
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch


class JoiningMeshNetworkProgressFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_joining_mesh_network_progress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fm = flowManagerVM.flowManager!!
        fm.meshSetupModule.commissionerStartedLD.observeForProgress(R.id.status_stage_1)
        fm.meshSetupModule.targetJoinedMeshNetworkLD.observeForProgress(R.id.status_stage_2)
        fm.cloudConnectionModule.targetOwnedByUserLD.observeForProgress(R.id.status_stage_3, 1000)
        fm.cloudConnectionModule.targetOwnedByUserLD.observeForProgress(R.id.status_stage_4, 2000)
    }

    private fun LiveData<Boolean?>.observeForProgress(
            @IdRes progressStage: Int,
            delayMillis: Int = 0
    ) {
        this.observe(
                this@JoiningMeshNetworkProgressFragment,
                Observer {
                    launch(UI) {
                        if (delayMillis > 0) {
                            delay(delayMillis)
                        }
                        markProgress(it, progressStage)
                    }
                }
        )
    }
}
