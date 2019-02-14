package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.afollestad.materialdialogs.MaterialDialog
import io.particle.mesh.R
import io.particle.mesh.R.string
import kotlinx.android.synthetic.main.fragment_new_mesh_network_password.view.*


class NewMeshNetworkPasswordFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_new_mesh_network_password, container, false)
        root.action_next.setOnClickListener { onNetworkPasswordEntered() }
        return root
    }

    private fun onNetworkPasswordEntered() {
        val password = view!!.networkPasswordInputLayout.editText!!.text.toString()
        val isValid = validateNetworkPassword(password)
        if (!isValid) {
            MaterialDialog.Builder(requireActivity())
                    .content(R.string.p_newmeshnetworkname_invalid_password_dialog_text)
                    .positiveText(android.R.string.ok)
                    .show()
            return
        }

        val confirmation = view!!.networkPasswordConfirmInputLayout.editText!!.text.toString()
        if (password != confirmation) {
            MaterialDialog.Builder(requireActivity())
                    .content(R.string.p_newmeshnetworkpassword_passwords_do_not_match_dialog_content)
                    .positiveText(android.R.string.ok)
                    .show()
            return
        }

        flowManagerVM.flowManager!!.meshSetupModule.updateNewNetworkPassword(password)
    }

    // FIXME: move this to the backend
    private fun validateNetworkPassword(password: String): Boolean {
        return password.length >= 6
    }
}
