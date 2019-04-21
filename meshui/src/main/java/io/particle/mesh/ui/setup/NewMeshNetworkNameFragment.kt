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
import kotlinx.android.synthetic.main.fragment_new_mesh_network_name.*


class NewMeshNetworkNameFragment : BaseFlowFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_mesh_network_name, container, false)
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        action_next.setOnClickListener { onNetworkNameEntered() }
    }

    private fun onNetworkNameEntered() {
        val name = networkNameInputLayout.editText!!.text.toString()
        val isValid = validateNetworkName(name)
        if (!isValid) {
            MaterialDialog.Builder(requireActivity())
                    .content(R.string.p_newmeshnetworkname_invalid_name_dialog_text)
                    .positiveText(android.R.string.ok)
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