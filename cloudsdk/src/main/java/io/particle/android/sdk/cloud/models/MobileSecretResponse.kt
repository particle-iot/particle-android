package io.particle.android.sdk.cloud.models

import com.google.gson.annotations.SerializedName


data class MobileSecretResponse(
    @SerializedName("mobile_secret")
    val fullMobileSecret: String?,
    val deviceId: String?
)