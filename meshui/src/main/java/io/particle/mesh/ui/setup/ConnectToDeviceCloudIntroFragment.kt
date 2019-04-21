package io.particle.mesh.ui.setup


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentActivity
import com.squareup.phrase.Phrase
import io.particle.mesh.common.QATool
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.setup.flow.Gen3ConnectivityType
import io.particle.mesh.setup.flow.context.NetworkSetupType
import io.particle.mesh.setup.flow.context.NetworkSetupType.STANDALONE
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.R
import io.particle.mesh.ui.setup.SetupType.ARGON_GATEWAY
import io.particle.mesh.ui.setup.SetupType.ARGON_STANDALONE
import io.particle.mesh.ui.setup.SetupType.BORON_GATEWAY_ACTIVE
import io.particle.mesh.ui.setup.SetupType.BORON_GATEWAY_NOT_ACTIVE
import io.particle.mesh.ui.setup.SetupType.BORON_STANDALONE_ACTIVE
import io.particle.mesh.ui.setup.SetupType.BORON_STANDALONE_NOT_ACTIVE
import kotlinx.android.synthetic.main.fragment_connect_to_device_cloud_intro.*


class ConnectToDeviceCloudIntroFragment : BaseFlowFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_connect_to_device_cloud_intro,
            container,
            false
        )
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        getSetupType()?.let { showSetupType(it) }

        p_action_next.setOnClickListener {
            val ful = this@ConnectToDeviceCloudIntroFragment.flowUiListener
            ful?.updateShouldConnectToDeviceCloudConfirmed(true)
        }
    }

    private fun getSetupType(): SetupType? {
        val target = flowUiListener?.targetDevice ?: return null
        val ful = flowUiListener ?: return null

        val networkSetupType = ful.deviceData.networkSetupTypeLD.value!!
        val isSimActive = target.isSimActivatedLD.value.truthy()

        return when (target.connectivityType!!) {
            Gen3ConnectivityType.MESH_ONLY -> throw IllegalStateException("Mesh-only is N/A here!")

            Gen3ConnectivityType.WIFI -> {
                if (networkSetupType == STANDALONE) ARGON_STANDALONE else ARGON_GATEWAY
            }

            Gen3ConnectivityType.CELLULAR -> {
                when (networkSetupType) {
                    NetworkSetupType.AS_GATEWAY -> {
                        if (isSimActive) BORON_GATEWAY_ACTIVE else BORON_GATEWAY_NOT_ACTIVE
                    }
                    NetworkSetupType.STANDALONE -> {
                        if (isSimActive) BORON_STANDALONE_ACTIVE else BORON_STANDALONE_NOT_ACTIVE
                    }
                    NetworkSetupType.NODE_JOINER -> {
                        throw IllegalStateException("Joiner flow does not apply here!")
                    }
                }
            }
        }
    }

    private fun showSetupType(setupType: SetupType) {
        setup_header_text.text = setupType.header.templated()
        p_action_next.setText(setupType.buttonLabel)
        p_mesh_step1.text = setupType.step1.templated()
        p_mesh_step2.text = setupType.step2.templated()
        p_mesh_step3.text = if (setupType.step3 == null) "" else setupType.step3.templated()
    }

    private fun Int.templated(): CharSequence {
        return Phrase.from(resources, this)
            .putOptional("product_type", getUserFacingTypeName())
            .format()
    }
}


internal enum class SetupType(
    @StringRes val header: Int,
    @StringRes val buttonLabel: Int,
    @StringRes val step1: Int,
    @StringRes val step2: Int,
    @StringRes val step3: Int?
) {
    ARGON_GATEWAY(
        R.string.p_argonconnecttocloud_header_gateway,
        R.string.p_action_next,
        R.string.p_argonconnecttocloud_step1_gateway,
        R.string.p_argonconnecttocloud_step2_gateway,
        R.string.p_argonconnecttocloud_step3_gateway
    ),

    ARGON_STANDALONE(
        R.string.p_argonconnecttocloud_header_standalone,
        R.string.p_action_next,
        R.string.p_argonconnecttocloud_step1_standalone,
        R.string.p_argonconnecttocloud_step2_standalone,
        null
    ),

    BORON_GATEWAY_ACTIVE(
        R.string.p_argonconnecttocloud_header_gateway,
        R.string.p_action_connect_to_device_cloud,
        R.string.p_argonconnecttocloud_step1_gateway,
        R.string.p_argonconnecttocloud_step2_boron_activated,
        R.string.p_argonconnecttocloud_step3_gateway
    ),

    BORON_STANDALONE_ACTIVE(
        R.string.p_argonconnecttocloud_header_standalone,
        R.string.p_action_connect_to_device_cloud,
        R.string.p_argonconnecttocloud_step1_standalone,
        R.string.p_argonconnecttocloud_step2_boron_activated,
        null
    ),

    BORON_GATEWAY_NOT_ACTIVE(
        R.string.p_argonconnecttocloud_header_gateway,
        R.string.p_action_activate_sim_and_connect,
        R.string.p_argonconnecttocloud_step1_gateway,
        R.string.p_argonconnecttocloud_step2_boron_not_activated,
        R.string.p_argonconnecttocloud_step3_gateway
    ),

    BORON_STANDALONE_NOT_ACTIVE(
        R.string.p_argonconnecttocloud_header_standalone,
        R.string.p_action_activate_sim_and_connect,
        R.string.p_argonconnecttocloud_step1_standalone,
        R.string.p_argonconnecttocloud_step2_boron_not_activated,
        null
    );

}
