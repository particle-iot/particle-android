package io.particle.android.sdk.cloud.models

import com.google.gson.annotations.SerializedName


data class DeviceIdentifiers(
    @SerializedName("device_id")
    val deviceId: String,

    @SerializedName("iccid")
    val iccid: String?,

    @SerializedName("platform_id")
    val platformId: Int
)
