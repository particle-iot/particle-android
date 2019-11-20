package io.particle.android.sdk.cloud

import com.google.gson.annotations.SerializedName
import java.util.*
import javax.annotation.ParametersAreNonnullByDefault

// Normally it's bad form to use network data models as API data models, but considering that
// for the moment, they'd be a 1:1 mapping, we'll just reuse this data model class.  If the
// network API changes, then we can write new classes for the network API models, without
// impacting the public API of the SDK.
@ParametersAreNonnullByDefault
class ParticleEvent(
    @SerializedName("coreid") val deviceId: String,
    @SerializedName("data") val dataPayload: String?,
    @SerializedName("published_at") val publishedAt: Date,
    @SerializedName("ttl") val timeToLive: Int?
)