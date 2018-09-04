package io.particle.mesh.setup.ui


import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.annotation.IdRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.firmwareprotos.ctrl.Common
import io.particle.mesh.common.QATool
import io.particle.mesh.common.Result
import io.particle.mesh.setup.utils.safeToast
import io.particle.sdk.app.R
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging
import java.io.IOException


class JoiningMeshNetworkProgressFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_joining_mesh_network_progress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fm = flowManagerVM.flowManager!!
        fm.commissionerStartedLD.observe(this, Observer { markProgress(R.id.status_stage_1) })
        fm.targetJoinedMeshNetworkLD.observe(this, Observer { markProgress(R.id.status_stage_2) })
        fm.targetOwnedByUserLD.observe(this, Observer { markProgress(R.id.status_stage_3) })
    }

    private fun markProgress(@IdRes progressStage: Int) {
        launch(UI) {
            val tv: TextView = view!!.findViewById(progressStage)
            tv.text = "✓ " + tv.text
        }
    }
}
