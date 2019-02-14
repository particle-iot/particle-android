package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.squareup.phrase.Phrase
import io.particle.android.sdk.cloud.ParticlePricingInfo
import io.particle.mesh.R
import kotlinx.android.synthetic.main.fragment_pricing_impact.*


class PricingImpactFragment : BaseMeshSetupFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pricing_impact, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fm = flowManagerVM.flowManager!!

        p_action_next.setOnClickListener {
            fm.cloudConnectionModule.updatePricingImpactConfirmed(true)
        }

        formatBasedOnPricingImpact(fm.cloudConnectionModule.pricingImpact!!)
    }

    private fun formatBasedOnPricingImpact(pricingInfo: ParticlePricingInfo) {
        val v = view

        // FIXME: FORMAT AS CURRENCY
        p_pricingimpact_price.text = Phrase.from(v, R.string.p_pricingimpact_PriceTextStrikethrough)
            .put("price", "${pricingInfo.plan.monthlyBaseAmount}")
            .format()

        p_action_next.setText(
            if (pricingInfo.chargeable) {
                R.string.p_action_enroll_in_subscription
            } else {
                R.string.p_action_next
            }
        )

        when (pricingInfo.planSlug) {
            "DeviceCloudCellularSelfServe" -> {  //add-device-to_user + cellular
                p_pricingimpact_header.setText(R.string.p_pricingimpact_PaidNetworkTitle)
                p_pricingimpact_header.setText(R.string.p_pricingimpact_PaidGatewayDeviceTitle)
                p_pricingimpact_subscription_type_header.setText(R.string.p_pricingimpact_DeviceCloudPlanTitle)
                p_pricingimpact_subscription_type_subheader.setText(R.string.p_pricingimpact_CellularDeviceText)

                p_pricingimpact_freebie_header.text = Phrase.from(v, R.string.p_pricingimpact_FreeMonthsText)
                    .put("free_months", pricingInfo.plan.freeMonths)
                    .format()
                p_pricingimpact_mesh_network_includes_divider.setText(R.string.p_pricingimpact_DeviceCloudFeatures)

                p_pricingimpact_feature1.setText(R.string.p_pricingimpact_FeaturesDeviceCloud)
                p_pricingimpact_feature2.text = Phrase.from(v, R.string.p_pricingimpact_FeaturesDataAllowence)
                    .put("emmbees", pricingInfo.plan.includedDataMb)
                    .put("per_mb_price", "${pricingInfo.plan.overageMinCostMb}") // FIXME: FORMAT AS CURRENCY
                    .format()
                p_pricingimpact_feature3.setText(R.string.p_pricingimpact_FeaturesStandardSupport)
            }

            "MeshMicroCellular" -> {    // create-network + cellular
                p_pricingimpact_header.setText(R.string.p_pricingimpact_PaidNetworkTitle)
                p_pricingimpact_subscription_type_header.setText(R.string.p_pricingimpact_MicroNetworkPlanTitle)
                p_pricingimpact_subscription_type_subheader.setText(R.string.p_pricingimpact_CellularGatewayText)

                p_pricingimpact_freebie_header.text = Phrase.from(v, R.string.p_pricingimpact_FreeMonthsText)
                            .put("free_months", pricingInfo.plan.freeMonths)
                            .format()
                p_pricingimpact_mesh_network_includes_divider.setText(R.string.p_pricingimpact_MeshNetworkFeatures)

                p_pricingimpact_feature1.setText(R.string.p_pricingimpact_FeaturesDeviceCloud)
                p_pricingimpact_feature2.text = Phrase.from(v, R.string.p_pricingimpact_FeaturesMaxDevices)
                    .put("total_devices", pricingInfo.plan.includedNodeCount)
                    .format()
                p_pricingimpact_feature3.text = Phrase.from(v, R.string.p_pricingimpact_FeaturesMaxGateways)
                    .put("total_gateways", pricingInfo.plan.includedGatewayCount)
                    .format()
                p_pricingimpact_feature4.text = Phrase.from(v, R.string.p_pricingimpact_FeaturesDataAllowence)
                    .put("emmbees", pricingInfo.plan.includedDataMb)
                    .put("per_mb_price", "${pricingInfo.plan.overageMinCostMb}") // FIXME: FORMAT AS CURRENCY
                    .format()
                p_pricingimpact_feature5.setText(R.string.p_pricingimpact_FeaturesStandardSupport)
            }

            "MeshMicroWifi" -> {  // create-network + wifi
                p_pricingimpact_header.setText(R.string.p_pricingimpact_FreeNetworkTitle)
                p_pricingimpact_subscription_type_header.setText(R.string.p_pricingimpact_MicroNetworkPlanTitle)
                p_pricingimpact_subscription_type_subheader.setText(R.string.p_pricingimpact_WifiGatewayText)

                p_pricingimpact_freebie_header.text = Phrase.from(v, R.string.p_pricingimpact_FreeNetworksText)
                    .put("free_networks", pricingInfo.plan.freeWifiNetworkMaxCount)
                    .format()
                p_pricingimpact_mesh_network_includes_divider.setText(R.string.p_pricingimpact_MeshNetworkFeatures)

                p_pricingimpact_feature1.setText(R.string.p_pricingimpact_FeaturesDeviceCloud)
                p_pricingimpact_feature2.text = Phrase.from(v, R.string.p_pricingimpact_FeaturesMaxDevices)
                    .put("total_devices", pricingInfo.plan.includedNodeCount)
                    .format()
                p_pricingimpact_feature3.text = Phrase.from(v, R.string.p_pricingimpact_FeaturesMaxGateways)
                    .put("total_gateways", pricingInfo.plan.includedGatewayCount)
                    .format()
                p_pricingimpact_feature4.setText(R.string.p_pricingimpact_FeaturesStandardSupport)
            }

            null -> {  // add-device-to-user + wifi
                p_pricingimpact_header.setText(R.string.p_pricingimpact_FreeGatewayDeviceTitle)
                p_pricingimpact_subscription_type_header.setText(R.string.p_pricingimpact_DeviceCloudPlanTitle)
                p_pricingimpact_subscription_type_subheader.setText(R.string.p_pricingimpact_WifiDeviceText)

                p_pricingimpact_freebie_header.text = Phrase.from(v, R.string.p_pricingimpact_FreeDevicesText)
                    .put("free_devices", pricingInfo.plan.freeDeviceMaxCount)
                    .format()
                p_pricingimpact_mesh_network_includes_divider.setText(R.string.p_pricingimpact_DeviceCloudFeatures)
                p_pricingimpact_feature1.setText(R.string.p_pricingimpact_FeaturesDeviceCloud)
                p_pricingimpact_feature2.setText(R.string.p_pricingimpact_FeaturesStandardSupport)
            }
        }

    }

}
