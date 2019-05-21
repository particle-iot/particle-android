package io.particle.android.sdk.cloud.models

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.particle.android.sdk.utils.UnknownEnumStringValueException
import io.particle.android.sdk.utils.buildStringValueMap
import java.io.IOException
import java.util.*


enum class ParticleApiSimStatus(val apiString: String) {
    ACTIVE("active"),
    INACTIVE_NEVER_ACTIVATED("never_before_activated"),
    INACTIVE_USER_DEACTIVATED("inactive_user_deactivated"),
    INACTIVE_DATA_LIMIT_REACHED("inactive_data_limit_reached"),
    INACTIVE_INVALID_PAYMENT_METHOD("inactive_invalid_payment_method");

    companion object {

        private val mapping = buildStringValueMap(ParticleApiSimStatus.values()) { it.apiString }

        fun fromString(stringFromApi: String): ParticleApiSimStatus? {
            return mapping[stringFromApi]
        }
    }
}


data class ParticleSim(

    @SerializedName("_id")
    val iccId: String,

    @SerializedName("activations_count")
    val activationsCount: Int,

    @SerializedName("base_country_code")
    val baseCountryCode: String,

    @SerializedName("base_monthly_rate")
    val baseMonthlyRateCentsPerMB: String,

    @SerializedName("deactivations_count")
    val deactivationsCount: String,

    @SerializedName("first_activated_on")
    val firstActivatedOn: Date?,

    @SerializedName("last_activated_on")
    val lastActivatedOn: Date?,

    @SerializedName("last_status_change_action")
    val lastStatusChangeAction: String?,

//    @SerializedName("last_status_change_action_error")
//    val iccId: String,

    @SerializedName("mb_limit")
    val monthlyDataRateLimitInMBs: Int,

    @SerializedName("msisdn")
    val msisdn: String?,

    @SerializedName("overage_monthly_rate")
    val overageMonthlyRateCentsPerMB: Int,

    @JsonAdapter(ParticleApiSimStatusAdapter::class)
    @SerializedName("status")
    val simStatus: ParticleApiSimStatus,

    @SerializedName("stripe_plan_slug")
    val stripePlanSlug: String?,

    @SerializedName("updated_at")
    val updatedAt: Date?,

    @SerializedName("user_id")
    val ownerUserId: String,

    @SerializedName("carrier")
    val carrier: String,

    @SerializedName("last_device_id")
    val lastDeviceId: String?,

    @SerializedName("last_device_name")
    val lastDeviceName: String?

)


internal class ParticleApiSimStatusAdapter : TypeAdapter<ParticleApiSimStatus?>() {

    companion object {
        private val wrappedGson = Gson()
    }

    @Throws(IOException::class)
    override fun write(writer: JsonWriter, value: ParticleApiSimStatus?) {
        if (value == null) {
            synchronized(this) {
                wrappedGson.toJson(value, writer)
            }
        } else {
            writer.value(value.apiString)
        }
    }

    @Throws(IOException::class)
    override fun read(reader: JsonReader): ParticleApiSimStatus? {
        val stringVal = reader.nextString()
        return ParticleApiSimStatus.fromString(stringVal)
    }

}