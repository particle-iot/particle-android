package io.particle.android.sdk.cloud

import com.google.gson.annotations.SerializedName
import io.particle.android.sdk.cloud.Responses.Models.CoreInfo
import io.particle.android.sdk.cloud.models.ParticleNetworkData
import java.util.*

/**
 * All API responses, collected together in one outer class for simplicity's sake.
 */
class Responses {

    /**
     * ...and to go with the responses, a series of model objects only
     * used internally when dealing with the REST API, never returned
     * outside of the cloudapi package.
     */
    class Models {

        class CoreInfo(
            @SerializedName("last_app")
            val lastApp: String,
            @SerializedName("last_heard")
            val lastHeard: Date,
            val connected: Boolean,
            val deviceId: String
        )

        /**
         * Represents a Particle device from "GET /v1/devices/{device id}"
         * and "GET /v1/devices"
         */
        class CompleteDevice internal constructor(
            @SerializedName("id")
            val deviceId: String,
            val name: String,
            @SerializedName("connected")
            val isConnected: Boolean,
            val cellular: Boolean,
            val imei: String,
            val iccid: String,
            @SerializedName("last_iccid")
            val lastIccid: String,
            @SerializedName("current_build_target")
            val currentBuild: String,
            @SerializedName("default_build_target")
            val defaultBuild: String,
            val variables: Map<String, String>?,
            val functions: List<String?>?,
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
            @SerializedName("last_heard")
            val lastHeard: Date,
            @SerializedName("serial_number")
            val serialNumber: String,
            @SerializedName("mobile_secret")
            val mobileSecret: String,
            @SerializedName("system_firmware_version")
            val systemFirmwareVersion: String,
            val notes: String?
        )

    }


    class TokenResponse(val token: String)


    class CallFunctionResponse(
        @SerializedName("id")
        val deviceId: String,
        @SerializedName("name")
        val deviceName: String,
        val connected: Boolean,
        @SerializedName("return_value")
        val returnValue: Int
    )


    class LogInResponse(
        @SerializedName("expires_in")
        val expiresInSeconds: Long,
        @SerializedName("access_token")
        val accessToken: String,
        @SerializedName("refresh_token")
        val refreshToken: String,
        @SerializedName("token_type")
        val tokenType: String
    )


    class SimpleResponse(val ok: Boolean, val error: String) {

        override fun toString(): String {
            return "SimpleResponse [ok=$ok, error=$error]"
        }
    }


    class ClaimCodeResponse(
        @SerializedName("claim_code")
        val claimCode: String,
        @SerializedName("device_ids")
        val deviceIds: Array<String>
    )

    class FirmwareUpdateInfoResponse(
        @SerializedName("binary_url")
        val nextFileUrl: String
    )

    abstract class ReadVariableResponse<T>(
        @SerializedName("cmd")
        val commandName: String,
        @SerializedName("name")
        val variableName: String,
        val coreInfo: Models.CoreInfo,
        val result: T
    )

    class ReadIntVariableResponse(
        commandName: String,
        variableName: String,
        coreInfo: CoreInfo,
        result: Int
    ) : ReadVariableResponse<Int>(commandName, variableName, coreInfo, result)


    class ReadDoubleVariableResponse(
        commandName: String,
        variableName: String,
        coreInfo: CoreInfo,
        result: Double
    ) : ReadVariableResponse<Double>(commandName, variableName, coreInfo, result)


    class ReadStringVariableResponse(
        commandName: String,
        variableName: String,
        coreInfo: CoreInfo,
        result: String
    ) : ReadVariableResponse<String>(commandName, variableName, coreInfo, result)


    class ReadObjectVariableResponse(
        commandName: String,
        variableName: String,
        coreInfo: CoreInfo,
        result: Any?
    ) : ReadVariableResponse<Any?>(commandName, variableName, coreInfo, result)


    class MeshNetworkRegistrationResponse(val network: RegisteredNetwork) {

        data class RegisteredNetwork(
            val id: String,
            val name: String,
            val state: ParticleNetworkState,
            val type: ParticleNetworkType
        )

    }

    data class DeviceMeshMembership(
        @SerializedName("id")
        val deviceId: String,
        @SerializedName("network")
        val membership: MeshMembership
    )

    data class MeshMembership(
        @SerializedName("id")
        val networkId: String,
        @SerializedName("role")
        val roleData: MeshRoleData
    )

    data class MeshRoleData(
        @SerializedName("gateway")
        val isGateway: Boolean
    )


    class PingResponse(val online: Boolean)


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

}
