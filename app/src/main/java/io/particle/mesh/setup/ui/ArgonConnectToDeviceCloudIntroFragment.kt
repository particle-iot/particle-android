package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import io.particle.mesh.setup.flow.modules.device.NetworkSetupType
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_argon_connect_to_device_cloud_intro.*


class ArgonConnectToDeviceCloudIntroFragment : BaseMeshSetupFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_argon_connect_to_device_cloud_intro,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val flowMan = flowManagerVM.flowManager

        val networkSetupType = flowMan?.deviceModule?.networkSetupTypeLD?.value!!
        val setupType = when (networkSetupType) {
            NetworkSetupType.AS_GATEWAY -> SetupType.GATEWAY
            NetworkSetupType.STANDALONE -> SetupType.STANDALONE
        }
        showSetupType(setupType)

        p_action_next.setOnClickListener {
            flowMan.cloudConnectionModule.updateShouldConnectToDeviceCloudConfirmed(true)
        }
    }

    private fun showSetupType(setupType: SetupType) {
        setup_header_text.setText(setupType.header)
        p_mesh_step1.setText(setupType.step1)
        p_mesh_step2.setText(setupType.step2)
        setupType.step3?.also { p_mesh_step3.setText(it) }
    }
}


internal enum class SetupType(
    @StringRes val header: Int,
    @StringRes val step1: Int,
    @StringRes val step2: Int,
    @StringRes val step3: Int?
) {
    GATEWAY(
        R.string.p_argonconnecttocloud_header_gateway,
        R.string.p_argonconnecttocloud_step1_gateway,
        R.string.p_argonconnecttocloud_step2_gateway,
        R.string.p_argonconnecttocloud_step3_gateway
    ),

    STANDALONE(
        R.string.p_argonconnecttocloud_header_standalone,
        R.string.p_argonconnecttocloud_step1_standalone,
        R.string.p_argonconnecttocloud_step2_standalone,
        null
    );

}
