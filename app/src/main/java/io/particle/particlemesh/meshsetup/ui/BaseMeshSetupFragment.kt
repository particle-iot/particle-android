package io.particle.particlemesh.meshsetup.ui

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import io.particle.particlemesh.meshsetup.MeshSetupController
import io.particle.particlemesh.meshsetup.MeshSetupStateViewModel


open class BaseMeshSetupFragment : Fragment() {

    protected lateinit var setupController: MeshSetupController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm = ViewModelProviders.of(requireActivity()).get(MeshSetupStateViewModel::class.java)
        setupController = vm.meshSetupController
    }

}