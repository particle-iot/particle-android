package io.particle.particlemesh.meshsetup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.particlemesh.common.QATool
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_name_your_device.view.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch


class NameYourDeviceFragment : BaseMeshSetupFragment() {

    private lateinit var cloud: ParticleCloud

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_name_your_device, container, false)

        root.action_next.setOnClickListener {
            val name = root.deviceNameInputLayout.editText!!.text.toString()
            launch {
                nameDevice(name)
            }
        }

        return root
    }

    private fun nameDevice(name: String) {
        val joinerDeviceId = setupController.deviceToBeSetUpParams.value!!.deviceId!!
        val joiner = cloud.getDevice(joinerDeviceId)
        QATool.runSafely({ joiner.setName(name) })
        launch(UI) {
            findNavController().navigate(R.id.action_nameYourDeviceFragment_to_setupFinishedFragment)
        }
    }

}
