package io.particle.mesh.setup.ui


import androidx.lifecycle.Observer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.ui.utils.markProgress
import io.particle.sdk.app.R
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch


class CreatingMeshNetworkFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_creating_mesh_network, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // this is an artificial distinction anyway, just mark it off.
        markProgress(true, R.id.status_stage_1)

        val fm = flowManagerVM.flowManager!!
        fm.meshSetupModule.createNetworkSent.observe(this, Observer { markProgress(it, R.id.status_stage_2) })
        fm.cloudConnectionModule.meshRegisteredWithCloud.observe(
                this,
                Observer {
                    markProgress(it, R.id.status_stage_3)
                    if (it.truthy()) {
                        launch(UI) { markFakeProgress() }
                    }
                }
        )
    }

    private suspend fun markFakeProgress() {
        delay(1000)
        markProgress(true, R.id.status_stage_5)
    }
}
