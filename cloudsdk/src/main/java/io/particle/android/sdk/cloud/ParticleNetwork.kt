package io.particle.android.sdk.cloud

import androidx.annotation.WorkerThread
import com.google.gson.annotations.SerializedName
import io.particle.android.sdk.cloud.models.ParticleNetworkData
import java.util.*


enum class ParticleNetworkState {
    @SerializedName("pending")
    PENDING,
    @SerializedName("confirmed")
    CONFIRMED;

    // thanks, GSON.
    override fun toString(): String {
        return when (this) {
            ParticleNetworkState.PENDING -> "pending"
            ParticleNetworkState.CONFIRMED -> "confirmed"
        }
    }
}


enum class ParticleNetworkType {
    @SerializedName("micro_wifi")
    MICRO_WIFI,

    @SerializedName("micro_cellular")
    MICRO_CELLULAR,

    @SerializedName("high_availability")
    HIGH_AVAILABILITY,

    @SerializedName("large_site")
    LARGE_SITE;

    // thanks, GSON.
    override fun toString(): String {
        return when (this) {
            ParticleNetworkType.MICRO_WIFI -> "micro_wifi"
            ParticleNetworkType.MICRO_CELLULAR -> "micro_cellular"
            ParticleNetworkType.HIGH_AVAILABILITY -> "high_availability"
            ParticleNetworkType.LARGE_SITE -> "large_site"
        }
    }

}



typealias NetworkID = String
typealias PanID = String
typealias XPanID = String


class ParticleNetwork internal constructor(
    private var data: ParticleNetworkData
) {

    val id: NetworkID
        get() = data.id
    val name: String
        get() = data.name
    val type: ParticleNetworkType
        get() = data.type
    val state: ParticleNetworkState
        get() = data.state
    val deviceCount: Int
        get() = data.deviceCount
    val gatewayCount: Int
        get() = data.gatewayCount
    val lastHeard: Date?
        get() = data.lastHeard
    val panId: PanID?
        get() = data.panId
    val xpanId: XPanID?
        get() = data.xpanId
    val channel: Int?
        get() = data.channel
    val notes: String?
        get() = data.notes

    @WorkerThread
    fun refresh() {
        TODO()
    }

    override fun toString(): String {
        return data.toString()
    }
}
