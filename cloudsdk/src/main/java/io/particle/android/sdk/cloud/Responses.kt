package io.particle.android.sdk.cloud

import com.google.gson.annotations.SerializedName
import java.util.*


data class CardOnFileResponse(
    val card: CardOnFile?
) {

    data class CardOnFile(

        val last4: String?,

        val brand: String?,

        @SerializedName("exp_month")
        val expiryMonthString: String?,

        @SerializedName("exp_year")
        val expiryYearString: String?,

        @SerializedName("id")
        val token: String?
    )
}


/**
 * A series of model objects only used internally when dealing with the REST API, never
 * returned outside of the cloudapi package.
 */
class Models {

    /** Represents a Particle device in the list returned by a call to "GET /v1/devices" */
    data class SimpleDevice(
        val id: String,
        val name: String,
        @SerializedName("connected")
        val isConnected: Boolean,
        val cellular: Boolean,
        val imei: String,
        @SerializedName("last_iccid")
        val lastIccid: String,
        @SerializedName("current_build_target")
        val currentBuild: String,
        @SerializedName("default_build_target")
        val defaultBuild: String,
        @SerializedName("platform_id")
        val platformId: Int,
        @field:SerializedName("product_id")
        val productId: Int,
        @SerializedName("last_ip_address")
        val ipAddress: String,
        @SerializedName("status")
        val status: String,
        @SerializedName("last_heard")
        val lastHeard: Date
    )

    /** Represents a Particle device as returned from the call to "GET /v1/devices/{device id}" */
    data class CompleteDevice(
        @SerializedName("id")
        val deviceId: String,
        val name: String,
        @SerializedName("connected")
        val isConnected: Boolean,
        val cellular: Boolean,
        val imei: String,
        @SerializedName("last_iccid")
        val lastIccid: String,
        @SerializedName("current_build_target")
        val currentBuild: String,
        @SerializedName("default_build_target")
        val defaultBuild: String,
        val variables: Map<String, String>,
        val functions: List<String>,
        @SerializedName("cc3000_patch_version")
        val version: String,
        @SerializedName("product_id")
        val productId: Int,
        @SerializedName("platform_id")
        val platformId: Int,
        @SerializedName("last_ip_address")
        val ipAddress: String,
        @SerializedName("last_app")
        val lastAppName: String,
        @SerializedName("status")
        val status: String,
        @SerializedName("device_needs_update")
        val requiresUpdate: Boolean,
        @SerializedName("last_heard")
        val lastHeard: Date
    )

}