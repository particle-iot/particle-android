package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.particle.android.sdk.utils.CoreNameGenerator
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_name_your_device.*
import kotlinx.android.synthetic.main.fragment_name_your_device.view.*


class NameYourDeviceFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_name_your_device, container, false)

        root.action_next.setOnClickListener {
            val name = root.deviceNameInputLayout.editText!!.text.toString()
            flowManagerVM.flowManager!!.cloudConnectionModule.updateTargetDeviceNameToAssign(name)
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val nameField = deviceNameInputLayout.editText!!
        if (nameField.text.isNullOrBlank()) {
            // FIXME: implement protection against duplicate naming
            val uniqueName = CoreNameGenerator.generateUniqueName(setOf())
            nameField.setText(uniqueName)
        }
    }
}
