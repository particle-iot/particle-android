package io.particle.android.sdk.cloud.models

import com.google.gson.annotations.SerializedName
import java.util.*


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

    @SerializedName("status")
    val simStatus: String,

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