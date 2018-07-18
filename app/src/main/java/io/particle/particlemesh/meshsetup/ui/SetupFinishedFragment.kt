package io.particle.particlemesh.meshsetup.ui


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_setup_finished.view.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch


class SetupFinishedFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_setup_finished, container, false)

        root.action_start_building.setOnClickListener{ endSetup(false) }
        root.action_add_next_mesh_device.setOnClickListener{ endSetup(true) }

        return root
    }

    private fun endSetup(restart: Boolean) {
        val appCtx = requireActivity().applicationContext
        setupController.clearData()
        requireActivity().finish()
        if (restart) {
            launch(UI) {
                delay(500)
                appCtx.startActivity(Intent(appCtx, MeshSetupActivity::class.java))
            }
        }
    }
}
