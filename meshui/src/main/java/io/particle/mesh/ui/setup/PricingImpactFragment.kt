package io.particle.mesh.ui.setup


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.squareup.phrase.Phrase
import io.particle.android.sdk.cloud.ParticlePricingInfo
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.R
import io.particle.mesh.ui.databinding.FragmentPricingImpactBinding
import java.text.NumberFormat


class PricingImpactFragment : BaseFlowFragment() {

    private var _binding: FragmentPricingImpactBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPricingImpactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        binding.pActionNext.setOnClickListener {
            this@PricingImpactFragment.flowUiListener?.cloud?.updatePricingImpactConfirmed(true)
        }

        formatBasedOnPricingImpact(flowUiListener.cloud.pricingImpact!!)
    }

    private fun formatBasedOnPricingImpact(pricingInfo: ParticlePricingInfo) {
        val v = view
        val currencyFormatter = NumberFormat.getCurrencyInstance()

        binding.pPricingimpactPrice.text = Phrase.from(v, R.string.p_pricingimpact_PriceTextStrikethrough)
            .put("price", currencyFormatter.format(pricingInfo.plan.monthlyBaseAmount))
            .format()

        binding.pActionNext.setText(
            if (pricingInfo.chargeable) {
                R.string.p_action_enroll_in_subscription
            } else {
                R.string.p_action_next
            }
        )

        when (pricingInfo.planSlug) {
            "DeviceCloudCellularSelfServe" -> {  //add-device-to_user + cellular
                binding.pPricingimpactHeader.setText(R.string.p_pricingimpact_PaidNetworkTitle)
                binding.pPricingimpactHeader.setText(R.string.p_pricingimpact_PaidGatewayDeviceTitle)
                binding.pPricingimpactSubscriptionTypeHeader.setText(R.string.p_pricingimpact_DeviceCloudPlanTitle)
                binding.pPricingimpactSubscriptionTypeSubheader.setText(R.string.p_pricingimpact_CellularDeviceText)

                binding.pPricingimpactFreebieHeader.text = Phrase.from(v, R.string.p_pricingimpact_FreeMonthsText)
                    .put("free_months", pricingInfo.plan.freeMonths)
                    .format()
                binding.pPricingimpactMeshNetworkIncludesDivider.setText(R.string.p_pricingimpact_DeviceCloudFeatures)

                binding.pPricingimpactFeature1.setText(R.string.p_pricingimpact_FeaturesDeviceCloud)
                binding.pPricingimpactFeature2.text = Phrase.from(v, R.string.p_pricingimpact_FeaturesDataAllowence)
                    .put("emmbees", pricingInfo.plan.includedDataMb)
                    .put("per_mb_price", currencyFormatter.format(pricingInfo.plan.overageMinCostMb))
                    .format()
                binding.pPricingimpactFeature3.setText(R.string.p_pricingimpact_FeaturesStandardSupport)
            }

            "MeshMicroCellular" -> {    // create-network + cellular
                binding.pPricingimpactHeader.setText(R.string.p_pricingimpact_PaidNetworkTitle)
                binding.pPricingimpactSubscriptionTypeHeader.setText(R.string.p_pricingimpact_MicroNetworkPlanTitle)
                binding.pPricingimpactSubscriptionTypeSubheader.setText(R.string.p_pricingimpact_CellularGatewayText)

                binding.pPricingimpactFreebieHeader.text = Phrase.from(v, R.string.p_pricingimpact_FreeMonthsText)
                            .put("free_months", pricingInfo.plan.freeMonths)
                            .format()
                binding.pPricingimpactMeshNetworkIncludesDivider.setText(R.string.p_pricingimpact_MeshNetworkFeatures)

                binding.pPricingimpactFeature1.setText(R.string.p_pricingimpact_FeaturesDeviceCloud)
                binding.pPricingimpactFeature2.text = Phrase.from(v, R.string.p_pricingimpact_FeaturesMaxDevices)
                    .put("total_devices", pricingInfo.plan.includedNodeCount)
                    .format()
                binding.pPricingimpactFeature3.text = Phrase.from(v, R.string.p_pricingimpact_FeaturesMaxGateways)
                    .put("total_gateways", pricingInfo.plan.includedGatewayCount)
                    .format()



                binding.pPricingimpactFeature4.text = Phrase.from(v, R.string.p_pricingimpact_FeaturesDataAllowence)
                    .put("emmbees", pricingInfo.plan.includedDataMb)
                    .put("per_mb_price", currencyFormatter.format(pricingInfo.plan.overageMinCostMb))
                    .format()
                binding.pPricingimpactFeature5.setText(R.string.p_pricingimpact_FeaturesStandardSupport)
            }

            "MeshMicroWifi" -> {  // create-network + wifi
                binding.pPricingimpactHeader.setText(R.string.p_pricingimpact_FreeNetworkTitle)
                binding.pPricingimpactSubscriptionTypeHeader.setText(R.string.p_pricingimpact_MicroNetworkPlanTitle)
                binding.pPricingimpactSubscriptionTypeSubheader.setText(R.string.p_pricingimpact_WifiGatewayText)

                binding.pPricingimpactFreebieHeader.text = Phrase.from(v, R.string.p_pricingimpact_FreeNetworksText)
                    .put("free_networks", pricingInfo.plan.freeWifiNetworkMaxCount)
                    .format()
                binding.pPricingimpactMeshNetworkIncludesDivider.setText(R.string.p_pricingimpact_MeshNetworkFeatures)

                binding.pPricingimpactFeature1.setText(R.string.p_pricingimpact_FeaturesDeviceCloud)
                binding.pPricingimpactFeature2.text = Phrase.from(v, R.string.p_pricingimpact_FeaturesMaxDevices)
                    .put("total_devices", pricingInfo.plan.includedNodeCount)
                    .format()
                binding.pPricingimpactFeature3.text = Phrase.from(v, R.string.p_pricingimpact_FeaturesMaxGateways)
                    .put("total_gateways", pricingInfo.plan.includedGatewayCount)
                    .format()
                binding.pPricingimpactFeature4.setText(R.string.p_pricingimpact_FeaturesStandardSupport)
            }

            null -> {  // add-device-to-user + wifi
                binding.pPricingimpactHeader.setText(R.string.p_pricingimpact_FreeGatewayDeviceTitle)
                binding.pPricingimpactSubscriptionTypeHeader.setText(R.string.p_pricingimpact_DeviceCloudPlanTitle)
                binding.pPricingimpactSubscriptionTypeSubheader.setText(R.string.p_pricingimpact_WifiDeviceText)

                binding.pPricingimpactFreebieHeader.text = Phrase.from(v, R.string.p_pricingimpact_FreeDevicesText)
                    .put("free_devices", pricingInfo.plan.freeDeviceMaxCount)
                    .format()
                binding.pPricingimpactMeshNetworkIncludesDivider.setText(R.string.p_pricingimpact_DeviceCloudFeatures)
                binding.pPricingimpactFeature1.setText(R.string.p_pricingimpact_FeaturesDeviceCloud)
                binding.pPricingimpactFeature2.setText(R.string.p_pricingimpact_FeaturesStandardSupport)
            }
        }

    }

}
