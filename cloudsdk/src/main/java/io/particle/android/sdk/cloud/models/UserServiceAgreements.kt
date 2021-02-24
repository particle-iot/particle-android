package io.particle.android.sdk.cloud.models

import com.google.gson.annotations.SerializedName

data class UserServiceAgreementsResponse(
    val data: List<UserServiceAgreementsData>
)


data class UserServiceAgreementsData(
    val type: String?,
    val attributes: ServiceAgreementsAttributes?
)


data class ServiceAgreementsAttributes(
    @SerializedName("agreement_type")
    val agreementType: String?,

    @SerializedName("current_usage_summary")
    val currentUsageSummary: UsageSummary?,

    @SerializedName("pricing_terms")
    val pricingTerms: PricingTerms?
)


data class UsageSummary(
    @SerializedName("device_limit_reached")
    val deviceLimitReached: Boolean?
)


data class PricingTerms(
    @SerializedName("device")
    val deviceTerms: DevicePricingTerms
)


data class DevicePricingTerms(
    @SerializedName("max_devices")
    val maxDevices: Int?
)
