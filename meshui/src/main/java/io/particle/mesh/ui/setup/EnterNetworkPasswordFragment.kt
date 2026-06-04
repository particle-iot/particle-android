package io.particle.mesh.ui.setup


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.databinding.FragmentEnterNetworkPasswordBinding


class EnterNetworkPasswordFragment : BaseFlowFragment() {

    private var _binding: FragmentEnterNetworkPasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentEnterNetworkPasswordBinding.inflate(inflater, container, false)
        binding.actionNext.setOnClickListener { onPasswordEntered() }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onPasswordEntered() {
        val password = binding.deviceNameInputLayout.editText!!.text.toString()
        flowUiListener?.mesh?.updateMeshNetworkToJoinCommissionerPassword(password)
    }
}
