package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.afollestad.materialdialogs.MaterialDialog
import io.particle.android.sdk.utils.Funcy.Predicate
import io.particle.mesh.R
import kotlinx.android.synthetic.main.fragment_new_mesh_network_name.view.*


class NewMeshNetworkNameFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_new_mesh_network_name, container, false)
        root.action_next.setOnClickListener { onNetworkNameEntered() }
        return root
    }

    private fun onNetworkNameEntered() {
        val name = view!!.networkNameInputLayout.editText!!.text.toString()
        val isValid = validateNetworkName(name)
        if (!isValid) {
            MaterialDialog.Builder(requireActivity())
                    .content(R.string.p_newmeshnetworkname_invalid_name_dialog_text)
                    .positiveText(android.R.string.ok)
                    .show()
            return
        }
        flowManagerVM.flowManager!!.meshSetupModule.updateNewNetworkName(name)
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