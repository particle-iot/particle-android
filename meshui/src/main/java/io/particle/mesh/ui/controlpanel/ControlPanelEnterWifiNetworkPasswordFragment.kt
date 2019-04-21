package io.particle.mesh.ui.controlpanel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.squareup.phrase.Phrase
import io.particle.mesh.ui.R
import kotlinx.android.synthetic.main.fragment_cp_enter_wifi_password.*


class ControlPanelEnterWifiNetworkPasswordFragment : BaseControlPanelFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_cp_enter_wifi_password, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val headerText = Phrase.from(view, io.particle.mesh.R.string.p_enterwifipassword_header)
            .put("wifi_ssid", flowUiListener?.wifi?.wifiNetworkToConfigure?.ssid)
            .format()
        setup_header_text.text = headerText

        action_next.setOnClickListener { setWifiPassword() }
    }

    private fun setWifiPassword() {
        val passwd = p_enterwifipassword_password_input.editText!!.text.toString()
        flowUiListener?.wifi?.setPasswordForWifiNetworkToConfigure(passwd)
    }
}