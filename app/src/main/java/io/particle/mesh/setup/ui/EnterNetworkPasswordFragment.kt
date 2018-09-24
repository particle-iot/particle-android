package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_enter_network_password.*
import kotlinx.android.synthetic.main.fragment_enter_network_password.view.*


class EnterNetworkPasswordFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_enter_network_password, container, false)
        root.action_next.setOnClickListener { onPasswordEntered() }
        return root
    }

    private fun onPasswordEntered() {
        val password = view!!.deviceNameInputLayout.editText!!.text.toString()
        val meshModule = flowManagerVM.flowManager!!.meshSetupModule
        meshModule.updateTargetMeshNetworkCommissionerPassword(password)
        // FIXME: REMOVE WHEN PROGRESS SPINNER IS ADDED
        action_next.isEnabled = false
    }
}
