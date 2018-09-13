package io.particle.mesh.setup.ui


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_gateway_setup_finished.view.*
import kotlinx.android.synthetic.main.fragment_joiner_setup_finished.view.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch


class GatewaySetupFinishedFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_gateway_setup_finished, container, false)

        root.action_start_building.setOnClickListener { endSetup(false) }
        root.action_start_mesh_setup.setOnClickListener { endSetup(true) }

        return root
    }

    // FIXME: this is duplicated with JoinerSetupFinishedFragment -- unify this
    private fun endSetup(restart: Boolean) {
        val appCtx = requireActivity().applicationContext
        flowManagerVM.flowManager?.clearState()
        requireActivity().finish()

        if (restart) {
            launch(UI) {
                delay(500)
                appCtx.startActivity(Intent(appCtx, MeshSetupActivity::class.java))
            }
        }
    }

}
