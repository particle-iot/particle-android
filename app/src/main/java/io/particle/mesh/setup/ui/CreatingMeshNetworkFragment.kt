package io.particle.mesh.setup.ui


import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.annotation.IdRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
        markProgress(R.id.status_stage_1)

        val fm = flowManagerVM.flowManager!!
        fm.meshSetupModule.createNetworkSent.observe(this, Observer { markProgress(R.id.status_stage_2) })
        fm.cloudConnectionModule.meshRegisteredWithCloud.observe(
                this,
                Observer {
                    markProgress(R.id.status_stage_3)
                    launch(UI) { markFakeProgress() }
                }
        )
    }

    private fun markProgress(@IdRes progressStage: Int) {
        launch(UI) {
            val tv: TextView? = view?.findViewById(progressStage)
            tv?.text = "✓ " + tv?.text
        }
    }

    private suspend fun markFakeProgress() {
        delay(1000)
        markProgress(R.id.status_stage_5)
    }
}
