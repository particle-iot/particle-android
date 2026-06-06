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
import io.particle.mesh.ui.R
import io.particle.mesh.ui.databinding.FragmentJoiningMeshNetworkProgressBinding
import io.particle.mesh.ui.utils.markProgress
import kotlinx.coroutines.delay


class JoiningMeshNetworkProgressFragment : BaseFlowFragment() {

    private var _binding: FragmentJoiningMeshNetworkProgressBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentJoiningMeshNetworkProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        observeForProgress(flowUiListener.mesh.commissionerStartedLD, R.id.status_stage_1) {
            observeForProgress(flowUiListener.mesh.targetJoinedMeshNetworkLD, R.id.status_stage_2) {
                observeForProgress(
                    flowUiListener.targetDevice.isClaimedLD,
                    R.id.status_stage_3,
                    1000
                ) {
                    // FIXME: WAT.  These shouldn't observe the same LiveData!
                    observeForProgress(
                        flowUiListener.targetDevice.isClaimedLD,
                        R.id.status_stage_4,
                        2000
                    )
                }
            }
        }

        val productName = getUserFacingTypeName()

        binding.setupHeaderText.text = Phrase.from(view, R.string.p_joiningmesh_header)
            .put("product_type", productName)
            .format()

        binding.statusStage1.text =
            Phrase.from(view, R.string.requesting_permission_to_add_to_mesh_network)
                .put("product_type", productName)
                .format()

        binding.statusStage2.text = Phrase.from(view, R.string.adding_the_xenon_to_the_mesh_network)
            .put("product_type", productName)
            .format()
    }
}

internal fun BaseFlowFragment.observeForProgress(
    liveData: LiveData<Boolean?>,
    @IdRes progressStage: Int,
    delayMillis: Long = 0,
    andThen: (() -> Unit)? = null
) {
    liveData.observe(
        this,
        Observer {
            if (it != true) {
                return@Observer
            }

            flowScopes.onMain {
                if (delayMillis > 0) {
                    delay(delayMillis)
                }
                markProgress(it, progressStage)
                andThen?.let { it() }
            }
        }
    )
}
