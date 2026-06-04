package io.particle.mesh.ui.controlpanel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.squareup.phrase.Phrase
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.R
import io.particle.mesh.ui.databinding.FragmentCpEnterWifiPasswordBinding


class ControlPanelEnterWifiNetworkPasswordFragment : BaseControlPanelFragment() {

    private var _binding: FragmentCpEnterWifiPasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCpEnterWifiPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)
        val headerText = Phrase.from(view, io.particle.mesh.R.string.p_enterwifipassword_header)
            .putOptional("wifi_ssid", flowUiListener.wifi.wifiNetworkToConfigure?.ssid)
            .format()
        binding.setupHeaderText.text = headerText

        binding.actionNext.setOnClickListener { setWifiPassword() }
    }

    private fun setWifiPassword() {
        val passwd = binding.pEnterwifipasswordPasswordInput.editText!!.text.toString()
        flowUiListener?.wifi?.setPasswordForWifiNetworkToConfigure(passwd)
    }
}