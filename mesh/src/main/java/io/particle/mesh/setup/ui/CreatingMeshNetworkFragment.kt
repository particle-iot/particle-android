package io.particle.mesh.setup.ui


import androidx.lifecycle.Observer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import com.squareup.phrase.Phrase
import io.particle.mesh.common.android.livedata.map
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.ui.utils.markProgress
import io.particle.mesh.R
import kotlinx.android.synthetic.main.fragment_creating_mesh_network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class CreatingMeshNetworkFragment : BaseMeshSetupFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_creating_mesh_network, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fm = flowManagerVM.flowManager!!

        // "Sending network credentials to your device"
        markProgress(true, R.id.status_stage_1) // this has already happened, just mark it off.

        // "Registering network with the cloud"
        fm.meshSetupModule.newNetworkIdLD.nonNull()
            .map { it.truthy() }
            .observeForProgress(R.id.status_stage_2)

        // "Device creating the mesh network locally"
        fm.meshSetupModule.createNetworkSent.observeForProgress(R.id.status_stage_3) {
            GlobalScope.launch(Dispatchers.Main) { markFakeProgress() }
        }

        status_stage_1.text = Phrase.from(view, R.string.p_creatingyournetwork_step_1)
            .put("product_type", fm.getTypeName())
            .format()
    }

    private suspend fun markFakeProgress() {
        delay(1500)
        if (isVisible) {
            markProgress(true, R.id.status_stage_4)
        }
        delay(1500)
        if (isVisible) {
            markProgress(true, R.id.status_stage_5)
        }
    }

    internal fun LiveData<Boolean?>.observeForProgress(
        @IdRes progressStage: Int,
        delayMillis: Long = 0,
        runAfter: (() -> Unit)? = null
    ) {
        this.observe(
            this@CreatingMeshNetworkFragment,
            Observer {
                GlobalScope.launch(Dispatchers.Main) {
                    if (delayMillis > 0) {
                        delay(delayMillis)
                    }
                    markProgress(it, progressStage)
                    runAfter?.invoke()
                }
            }
        )
    }

}
