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
import kotlinx.android.synthetic.main.fragment_name_your_device.*


class NameYourDeviceFragment : BaseFlowFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_name_your_device, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setup_header_text.text = Phrase.from(view, R.string.p_namedevice_header)
            .put("product_type", getUserFacingTypeName())
            .format()

        // get the current name
        val currentName = flowUiListener?.targetDevice?.currentDeviceName ?: ""

        // set the current name on the field
        val nameField = deviceNameInputLayout.editText!!
        nameField.setText(currentName)

        // is the name blank/null/empty?  Make one up.
        if (nameField.text.isNullOrBlank()) {
            // FIXME: implement protection against duplicate naming
            val uniqueName = CoreNameGenerator.generateUniqueName(setOf())
            nameField.setText(uniqueName)
        }
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        action_next.setOnClickListener {
            val name = deviceNameInputLayout.editText!!.text.toString()
            this@NameYourDeviceFragment.flowUiListener?.cloud?.updateTargetDeviceNameToAssign(name)
        }
    }

}
