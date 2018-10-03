package io.particle.mesh.setup.ui

import android.os.Bundle


open class BaseMeshSetupFragment : androidx.fragment.app.Fragment() {

    protected lateinit var flowManagerVM: FlowManagerAccessModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        flowManagerVM = FlowManagerAccessModel.getViewModel(this)
    }

}