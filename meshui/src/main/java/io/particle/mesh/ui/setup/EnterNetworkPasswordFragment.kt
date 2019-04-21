package io.particle.mesh.ui.setup


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.R
import kotlinx.android.synthetic.main.fragment_enter_network_password.view.*


class EnterNetworkPasswordFragment : BaseFlowFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_enter_network_password, container, false)
        root.action_next.setOnClickListener { onPasswordEntered() }
        return root
    }

    private fun onPasswordEntered() {
        val password = view!!.deviceNameInputLayout.editText!!.text.toString()
        flowUiListener?.mesh?.updateMeshNetworkToJoinCommissionerPassword(password)
    }
}
