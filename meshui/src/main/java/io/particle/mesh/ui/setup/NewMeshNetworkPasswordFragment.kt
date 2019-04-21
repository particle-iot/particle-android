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
import kotlinx.android.synthetic.main.fragment_new_mesh_network_password.*


class NewMeshNetworkPasswordFragment : BaseFlowFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_mesh_network_password, container, false)
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        action_next.setOnClickListener { onNetworkPasswordEntered() }
    }

    private fun onNetworkPasswordEntered() {
        val password = networkPasswordInputLayout.editText!!.text.toString()
        val isValid = validateNetworkPassword(password)
        if (!isValid) {
            MaterialDialog.Builder(requireActivity())
                    .content(R.string.p_newmeshnetworkname_invalid_password_dialog_text)
                    .positiveText(android.R.string.ok)
                    .show()
            return
        }

        val confirmation = networkPasswordConfirmInputLayout.editText!!.text.toString()
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
