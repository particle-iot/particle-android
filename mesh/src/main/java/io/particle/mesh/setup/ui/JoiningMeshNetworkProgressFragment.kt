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
import kotlinx.android.synthetic.main.fragment_joining_mesh_network_progress.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


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

        val productName = fm.getTypeName()

        setup_header_text.text = Phrase.from(view, R.string.p_joiningmesh_header)
                .put("product_type", productName)
                .format()

        status_stage_1.text = Phrase.from(view, R.string.requesting_permission_to_add_to_mesh_network)
                .put("product_type", productName)
                .format()

        status_stage_2.text = Phrase.from(view, R.string.adding_the_xenon_to_the_mesh_network)
                .put("product_type", productName)
                .format()
    }

    internal fun LiveData<Boolean?>.observeForProgress(
            @IdRes progressStage: Int,
            delayMillis: Long = 0
    ) {
        this.observe(
                this@JoiningMeshNetworkProgressFragment,
                Observer {
                    GlobalScope.launch(Dispatchers.Main) {
                        if (delayMillis > 0) {
                            delay(delayMillis)
                        }
                        markProgress(it, progressStage)
                    }
                }
        )
    }
}
