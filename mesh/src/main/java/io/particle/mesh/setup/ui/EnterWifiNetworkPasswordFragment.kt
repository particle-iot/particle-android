package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.squareup.phrase.Phrase
import io.particle.mesh.setup.flow.modules.cloudconnection.ArgonSteps
import io.particle.mesh.R
import kotlinx.android.synthetic.main.fragment_enter_wifi_network_password.*


class EnterWifiNetworkPasswordFragment : BaseMeshSetupFragment() {

    private lateinit var argonSteps: ArgonSteps

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_enter_wifi_network_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        argonSteps = flowManagerVM.flowManager!!.cloudConnectionModule.argonSteps

        val headerText = Phrase.from(view, R.string.p_enterwifipassword_header)
            .put("wifi_ssid", argonSteps.targetWifiNetwork.value!!.ssid)
            .format()
        setup_header_text.text = headerText

        action_next.setOnClickListener { setWifiPassword() }
    }

    private fun setWifiPassword() {
        argonSteps.updateTargetWifiNetworkPassword(
            p_enterwifipassword_password_input.editText!!.text.toString()
        )
    }
}
