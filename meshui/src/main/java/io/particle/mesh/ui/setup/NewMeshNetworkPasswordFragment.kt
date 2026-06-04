package io.particle.mesh.ui.setup


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.afollestad.materialdialogs.MaterialDialog
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.R
import io.particle.mesh.ui.databinding.FragmentNewMeshNetworkPasswordBinding


class NewMeshNetworkPasswordFragment : BaseFlowFragment() {

    private var _binding: FragmentNewMeshNetworkPasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentNewMeshNetworkPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        binding.actionNext.setOnClickListener { onNetworkPasswordEntered() }
    }

    private fun onNetworkPasswordEntered() {
        val password = binding.networkPasswordInputLayout.editText!!.text.toString()
        val isValid = validateNetworkPassword(password)
        if (!isValid) {
            MaterialDialog.Builder(requireActivity())
                    .content(R.string.p_newmeshnetworkname_invalid_password_dialog_text)
                    .positiveText(android.R.string.ok)
                    .show()
            return
        }

        val confirmation = binding.networkPasswordConfirmInputLayout.editText!!.text.toString()
        if (password != confirmation) {
            MaterialDialog.Builder(requireActivity())
                    .content(R.string.p_newmeshnetworkpassword_passwords_do_not_match_dialog_content)
                    .positiveText(android.R.string.ok)
                    .show()
            return
        }

        flowUiListener?.mesh?.updateNewNetworkPassword(password)
    }

    // FIXME: move this to the backend
    private fun validateNetworkPassword(password: String): Boolean {
        return password.length >= 6
    }
}
