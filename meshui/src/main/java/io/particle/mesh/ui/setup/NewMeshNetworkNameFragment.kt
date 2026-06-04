package io.particle.mesh.ui.setup


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.R
import io.particle.mesh.ui.databinding.FragmentNewMeshNetworkNameBinding


class NewMeshNetworkNameFragment : BaseFlowFragment() {

    private var _binding: FragmentNewMeshNetworkNameBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentNewMeshNetworkNameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        binding.actionNext.setOnClickListener { onNetworkNameEntered() }
    }

    private fun onNetworkNameEntered() {
        val name = binding.networkNameInputLayout.editText!!.text.toString()
        val isValid = validateNetworkName(name)
        if (!isValid) {
            MaterialAlertDialogBuilder(requireActivity())
                    .setMessage(R.string.p_newmeshnetworkname_invalid_name_dialog_text)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            return
        }
        flowUiListener?.mesh?.updateNewNetworkName(name)
    }

    private fun validateNetworkName(name: String): Boolean {
        val validations = listOf<(String) -> Boolean>(
                { it.length <= 16 },
                { it.isNotBlank() },
                { hasValidCharacters(it) }
        )
        for (v in validations) {
            if (!v(name)) {
                return false
            }
        }
        return true
    }

    private fun hasValidCharacters(name: String): Boolean {
        for (chr in name.iterator()) {
            if (!ALLOWABLE_CHARS.contains(chr, ignoreCase = true)) {
                return false
            }
        }
        return true
    }
}


// FIXME: move all this validation to the backend
private val ALLOWABLE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_"