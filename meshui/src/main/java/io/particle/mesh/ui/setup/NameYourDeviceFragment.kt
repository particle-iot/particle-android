package io.particle.mesh.ui.setup


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.squareup.phrase.Phrase
import io.particle.android.sdk.utils.CoreNameGenerator
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.R
import io.particle.mesh.ui.databinding.FragmentNameYourDeviceBinding


class NameYourDeviceFragment : BaseFlowFragment() {

    private var _binding: FragmentNameYourDeviceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNameYourDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        binding.setupHeaderText.text = Phrase.from(view, R.string.p_namedevice_header)
            .put("product_type", getUserFacingTypeName())
            .format()

        // get the current name
        val currentName = flowUiListener.targetDevice.currentDeviceName ?: ""

        // set the current name on the field
        val nameField = binding.deviceNameInputLayout.editText!!
        nameField.setText(currentName)

        // is the name blank/null/empty?  Make one up.
        if (nameField.text.isNullOrBlank()) {
            // FIXME: implement protection against duplicate naming
            val uniqueName = CoreNameGenerator.generateUniqueName(setOf())
            nameField.setText(uniqueName)
        }
        binding.actionNext.setOnClickListener {
            val name = binding.deviceNameInputLayout.editText!!.text.toString()
            this@NameYourDeviceFragment.flowUiListener?.cloud?.updateTargetDeviceNameToAssign(name)
        }
    }

}
