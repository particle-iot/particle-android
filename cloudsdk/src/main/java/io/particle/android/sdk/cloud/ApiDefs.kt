package io.particle.android.sdk.cloud

import io.particle.android.sdk.cloud.Responses.CallFunctionResponse
import io.particle.android.sdk.cloud.Responses.CardOnFileResponse
import io.particle.android.sdk.cloud.Responses.ClaimCodeResponse
import io.particle.android.sdk.cloud.Responses.DeviceMeshMembership
import io.particle.android.sdk.cloud.Responses.FirmwareUpdateInfoResponse
import io.particle.android.sdk.cloud.Responses.MeshNetworkRegistrationResponse
import io.particle.android.sdk.cloud.Responses.Models
import io.particle.android.sdk.cloud.Responses.PingResponse
import io.particle.android.sdk.cloud.Responses.ReadDoubleVariableResponse
import io.particle.android.sdk.cloud.Responses.ReadIntVariableResponse
import io.particle.android.sdk.cloud.Responses.ReadObjectVariableResponse
import io.particle.android.sdk.cloud.Responses.ReadStringVariableResponse
import io.particle.android.sdk.cloud.Responses.SimpleResponse
import io.particle.android.sdk.cloud.models.*
import retrofit.client.Response
import retrofit.http.*
import retrofit.mime.TypedOutput


/** Particle cloud REST APIs, modelled for the Retrofit library */
class ApiDefs {

    /** The main Particle cloud API */
    internal interface CloudApi {

        @GET("/v1/sims/{lastIccid}/data_usage")
        fun getCurrentDataUsage(@Path("lastIccid") lastIccid: String): Response

        @GET("/v1/devices")
        fun getDevices(): List<Models.CompleteDevice>

        @GET("/v1/devices/{deviceID}")
        fun getDevice(@Path("deviceID") deviceID: String): Models.CompleteDevice

        // FIXME: put a real response type on this?
        @FormUrlEncoded
        @PUT("/v1/devices/{deviceID}")
        fun nameDevice(@Path("deviceID") deviceID: String, @Field("name") name: String): Response

        @FormUrlEncoded
        @PUT("/v1/devices/{deviceID}")
        fun flashKnownApp(
            @Path("deviceID") deviceID: String,
            @Field("app") appName: String
        ): Response

        @Multipart
        @PUT("/v1/devices/{deviceID}")
        fun flashFile(@Path("deviceID") deviceID: String, @Part("file") file: TypedOutput): Response

        @POST("/v1/devices/{deviceID}/{function}")
        fun callFunction(
            @Path("deviceID") deviceID: String,
            @Path("function") function: String,
            @Body args: FunctionArgs
        ): CallFunctionResponse

        @GET("/v1/devices/{deviceID}/{variable}")
        fun getVariable(
            @Path("deviceID") deviceID: String,
            @Path("variable") variable: String
        ): ReadObjectVariableResponse

        @GET("/v1/devices/{deviceID}/{variable}")
        fun getIntVariable(
            @Path("deviceID") deviceID: String,
            @Path("variable") variable: String
        ): ReadIntVariableResponse

        @GET("/v1/devices/{deviceID}/{variable}")
        fun getStringVariable(
            @Path("deviceID") deviceID: String,
            @Path("variable") variable: String
        ): ReadStringVariableResponse

        @GET("/v1/devices/{deviceID}/{variable}")
        fun getDoubleVariable(
            @Path("deviceID") deviceID: String,
            @Path("variable") variable: String
        ): ReadDoubleVariableResponse

        @FormUrlEncoded
        @POST("/v1/devices/events")
        fun publishEvent(
            @Field("name") eventName: String,
            @Field("data") eventData: String?,
            @Field("private") isPrivate: Boolean,
            @Field("ttl") timeToLive: Int
        ): SimpleResponse

        /**
         * Newer versions of OkHttp *require* a body for POSTs, but just pass in
         * a blank string for the body and all is well.
         */
        @FormUrlEncoded
        @POST("/v1/device_claims")
        fun generateClaimCode(@Field("blank") blankBody: String): ClaimCodeResponse

        @FormUrlEncoded
        @POST("/v1/products/{productId}/device_claims")
        fun generateClaimCodeForOrg(
            @Field("blank") blankBody: String,
            @Path("productId") productId: Int?
        ): ClaimCodeResponse

        @FormUrlEncoded
        @POST("/v1/orgs/{orgSlug}/products/{productSlug}/device_claims")
        @Deprecated("")
        fun generateClaimCodeForOrg(
            @Field("blank") blankBody: String,
            @Path("orgSlug") orgSlug: String,
            @Path("productSlug") productSlug: String
        ): ClaimCodeResponse

        @FormUrlEncoded
        @POST("/v1/devices")
        fun claimDevice(@Field("id") deviceID: String): SimpleResponse

        @DELETE("/v1/devices/{deviceID}")
        fun unclaimDevice(@Path("deviceID") deviceID: String): SimpleResponse

        @GET("/v1/networks")
        fun getNetworks(): List<ParticleNetworkData>

        @GET("/v1/networks/{networkId}")
        fun getNetwork(@Path("networkId") networkId: String): ParticleNetworkData

        @GET("/v1/networks/{networkId}/devices")
        fun getNetworkDevices(@Path("networkId") networkId: String): List<DeviceMeshMembership>

        @FormUrlEncoded
        @POST("/v1/networks")
        fun registerMeshNetwork(
            @Field("deviceID") gatewayDeviceId: String,
            @Field("type") networkType: ParticleNetworkType,
            @Field("name") networkName: String
        ): MeshNetworkRegistrationResponse

        @FormUrlEncoded
        @POST("/v1/networks")
        fun registerMeshNetwork(
            @Field("deviceID") gatewayDeviceId: String,
            @Field("type") networkType: ParticleNetworkType,
            @Field("name") networkName: String,
            @Field("iccid") iccId: String
        ): MeshNetworkRegistrationResponse

        @PUT("/v1/networks/{network_id}")
        fun modifyMeshNetwork(
            @Path("network_id") networkId: String,
            @Body change: MeshNetworkChange
        ): Response

        @DELETE("/v1/devices/{deviceId}/network")
        fun removeDeviceFromAnyNetwork(@Path("deviceId") deviceId: String): Response

        @GET("/v1/system_firmware/upgrade")
        fun getFirmwareUpdateInfo(
            @Query("platform_id") platformId: Int,
            @Query("current_system_firmware_version") currentSystemFwVersion: String,
            @Query("current_ncp_firmware_version") currentNcpFwVersion: String?,
            @Query("current_ncp_firmware_module_version") currentNcpFwModuleVersion: Int?
        ): FirmwareUpdateInfoResponse?

        @GET("/v1/card")
        fun getPaymentCard(): CardOnFileResponse

        @HEAD("/v1/sims/{iccid}")
        fun checkSim(@Path("iccid") iccid: String): Response

        @FormUrlEncoded
        @PUT("/v1/sims/{iccid}")
        fun takeActionOnSim(
            @Path("iccid") iccid: String,
            @Field("action") action: String,
            @Field("mb_limit") limitInMBsForUnpause: Int? = null,
            @Field("country") isoAlpha2CountryCode: String? = "US"
        ): Response

        @FormUrlEncoded
        @PUT("/v1/sims/{iccid}")
        fun setDataLimit(@Path("iccid") iccid: String, @Field("mb_limit") limitInMBs: Int): Response

        @GET("/v1/sims/{iccid}")
        fun getSim(@Path("iccid") iccid: String): ParticleSim

        @GET("/v1/pricing-impact")
        fun getPricingImpact(
            @Query("action") actionString: String,
            @Query("plan") networkType: String,
            @Query("device_id") deviceId: String? = null,
            @Query("network_id") networkId: String? = null,
            @Query("iccid") iccid: String? = null
        ): ParticlePricingInfo

        @GET("/v1/serial_numbers/{serial_number}")
        fun getFullMobileSecret(
            @Path("serial_number") serialNumber: String,
            @Query("mobile_secret") partialMobileSecret: String
        ): MobileSecretResponse

        @GET("/v1/serial_numbers/{serial_number}")
        fun getDeviceIdentifiers(@Path("serial_number") serialNumber: String): DeviceIdentifiers

        @FormUrlEncoded
        @PUT("/v1/devices/{deviceID}")
        fun shoutRainbows(
            @Path("deviceID") deviceID: String,
            @Field("signal") shouldSignal: Int  // "0" for no, "1" for yes
        ): Response

        @FormUrlEncoded
        @PUT("/v1/devices/{deviceID}/ping")
        fun pingDevice(
            @Path("deviceID") deviceID: String,
            @Field("blank") blankBody: String
        ): PingResponse

        @FormUrlEncoded
        @PUT("/v1/devices/{deviceID}")
        fun setDeviceNote(
            @Path("deviceID") deviceID: String,
            @Field("notes") note: String
        ): Response

        @GET("/v1/user/service_agreements")
        fun getServiceAgreements(): UserServiceAgreementsResponse

    }


    /**
     * APIs dealing with identity and authorization
     *
     * These are separated out from the main API, since they aren't
     * authenticated like the main API, and as such need different
     * headers.
     */
    interface IdentityApi {

        @POST("/v1/users")
        fun signUp(@Body signUpInfo: SignUpInfo): Response

        // NOTE: the `LogInResponse` used here as a return type is intentional.  It looks
        // a little odd, but that's how this endpoint works.
        @POST("/v1/products/{productId}/customers")
        fun signUpAndLogInWithCustomer(
            @Body signUpInfo: SignUpInfo,
            @Path("productId") productId: Int?
        ): Responses.LogInResponse

        // NOTE: the `LogInResponse` used here as a return type is intentional.  It looks
        // a little odd, but that's how this endpoint works.
        @POST("/v1/orgs/{orgSlug}/customers")
        @Deprecated("")
        fun signUpAndLogInWithCustomer(
            @Body signUpInfo: SignUpInfo,
            @Path("orgSlug") orgSlug: String
        ): Responses.LogInResponse

        @FormUrlEncoded
        @POST("/oauth/token")
        fun logIn(
            @Field("grant_type") grantType: String,
            @Field("username") username: String,
            @Field("password") password: String
        ): Responses.LogInResponse

        @FormUrlEncoded
        @POST("/oauth/token")
        fun authenticate(
            @Field("grant_type") grantType: String,
            @Field("mfa_token") mfaToken: String,
            @Field("otp") otp: String
        ): Responses.LogInResponse

        @FormUrlEncoded
        @POST("/oauth/token")
        fun logIn(
            @Field("grant_type") grantType: String,
            @Field("refresh_token") refreshToken: String
        ): Responses.LogInResponse

        @FormUrlEncoded
        @POST("/v1/user/password-reset")
        fun requestPasswordReset(@Field("username") email: String): Response

    }
}
