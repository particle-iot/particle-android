package io.particle.android.sdk.cloud

import com.google.gson.annotations.SerializedName


enum class PricingImpactAction(val apiString: String) {
    ADD_USER_DEVICE("add-device-to-user"),          // standalone
    ADD_NETWORK_DEVICE("add-device-to-network"),    // mesh network joiner flow
    CREATE_NETWORK("create-network")                // mesh network creation flow
}


enum class PricingImpactNetworkType(val apiString: String) {
    WIFI("wifi"),
    CELLULAR("cellular")
}


data class ParticlePricingPlanInfo(
    @SerializedName("free_device_max_count")
    val freeDeviceMaxCount: Int,

    @SerializedName("free_devices_available_count")
    val freeDevicesAvailableCount: Int,

    @SerializedName("free_wifi_network_max_count")
    val freeWifiNetworkMaxCount: Int,

    @SerializedName("free_wifi_networks_available_count")
    val freeWifiNetworksAvailableCount: Int,

    @SerializedName("included_node_count")
    val includedNodeCount: Int,

    @SerializedName("included_gateway_count")
    val includedGatewayCount: Int,

    @SerializedName("included_data_mb")
    val includedDataMb: Int,

    @SerializedName("free_months")
    val freeMonths: Int,

    @SerializedName("monthly_base_amount")
    val monthlyBaseAmount: Float,

    @SerializedName("overage_min_cost_mb")
    val overageMinCostMb: Float
)



data class ParticlePricingInfo(
    @SerializedName("cc_last4")
    val ccLast4: String,

    @SerializedName("plan_slug")
    val planSlug: String?,

    val plan: ParticlePricingPlanInfo,

    val allowed: Boolean = true,

    val chargeable: Boolean = true,

    @SerializedName("cc_on_file")
    val ccOnFile: Boolean = true,

    val planUpgradeNeeded: Boolean = true
)
