package io.particle.android.sdk.cloud.models

import com.google.gson.annotations.SerializedName
import io.particle.android.sdk.cloud.*
import java.util.*


internal data class ParticleNetworkData(
    val id: NetworkID,
    val name: String,
    val type: ParticleNetworkType,
    val state: ParticleNetworkState,
    @SerializedName("device_count") val deviceCount: Int,
    @SerializedName("gateway_count") val gatewayCount: Int,
    @SerializedName("last_heard") val lastHeard: Date?,
    @SerializedName("pan_id") val panId: PanID?,
    @SerializedName("xpan_id") val xpanId: XPanID?,
    val channel: Int?,
    val notes: String?
)
