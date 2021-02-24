package io.particle.android.sdk.cloud

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import com.google.gson.Gson
import com.squareup.okhttp.HttpUrl
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.cloud.ParticleDevice.VariableType
import io.particle.android.sdk.cloud.Responses.CardOnFileResponse
import io.particle.android.sdk.cloud.Responses.DeviceMeshMembership
import io.particle.android.sdk.cloud.Responses.MeshNetworkRegistrationResponse.RegisteredNetwork
import io.particle.android.sdk.cloud.Responses.Models.CompleteDevice
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.android.sdk.cloud.exceptions.ParticleLoginException
import io.particle.android.sdk.cloud.models.*
import io.particle.android.sdk.persistance.AppDataStorage
import io.particle.android.sdk.utils.Broadcaster
import io.particle.android.sdk.utils.Py.all
import io.particle.android.sdk.utils.Py.truthy
import mu.KotlinLogging
import org.json.JSONException
import org.json.JSONObject
import retrofit.RetrofitError
import retrofit.RetrofitError.Kind
import retrofit.client.Response
import retrofit.mime.TypedByteArray
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import java.util.concurrent.ExecutorService
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set


// FIXME: move device state management out to another class
// FIXME: move some of the type conversion junk out of this into another class, too


class ParticleCloud internal constructor(
    schemeAndHostname: HttpUrl,
    private val mainApi: ApiDefs.CloudApi,
    private val identityApi: ApiDefs.IdentityApi,
    private val appDataStorage: AppDataStorage,
    private val broadcastManager: Broadcaster,

    gson: Gson,
    executor: ExecutorService
) {

    private val tokenDelegate = TokenDelegate()
    private val eventsDelegate: EventsDelegate

    private val devices = ArrayMap<String, ParticleDevice>()

    @Volatile
    private var token: ParticleAccessToken? = null
    @Volatile
    private var user: ParticleUser? = null

    //region general public API

    /**
     * Current session access token string.  Can be null.
     */
    //Adding 20 years to current time to create date in distant future
    var accessToken: String?
        get() = if (this.token == null) null else this.token!!.accessToken
        set(tokenString) {
            val distantFuture = Calendar.getInstance()
            distantFuture.add(Calendar.YEAR, 20)
            setAccessToken(tokenString!!, distantFuture.time, null)
        }

    /**
     * Currently logged in user name, or null if no session exists
     */
    val loggedInUsername: String?
        get() = if (all(this.token, this.user)) this.user!!.user else null

    val isLoggedIn: Boolean
        get() = loggedInUsername != null

    init {
        this.user = ParticleUser.fromSavedSession()
        this.token = ParticleAccessToken.fromSavedSession()
        if (this.token != null) {
            this.token!!.delegate = TokenDelegate()
        }
        this.eventsDelegate = EventsDelegate(mainApi, schemeAndHostname, gson, executor, this)
    }

    @JvmOverloads
    fun setAccessToken(
        tokenString: String,
        expirationDate: Date,
        refreshToken: String? = null
    ) {
        ParticleAccessToken.removeSession()
        this.token = ParticleAccessToken.fromTokenData(expirationDate, tokenString, refreshToken)
        this.token!!.delegate = tokenDelegate
    }

    /**
     * Login with existing account credentials to Particle cloud
     *
     * @param user     User name, must be a valid email address
     * @param password Password
     */
    @WorkerThread
    @Throws(ParticleLoginException::class)
    fun logIn(user: String, password: String) {
        try {
            val response = identityApi.logIn("password", user, password)
            onLogIn(response, user, password)
        } catch (error: RetrofitError) {
            throw ParticleLoginException(error)
        }

    }

    /**
     * Login with existing account credentials to Particle cloud
     *
     * @param user     User name, must be a valid email address
     * @param password Password
     * @param mfaToken Multi factor authentication token from server.
     * @param otp      One time password from authentication app.
     */
    @WorkerThread
    @Throws(ParticleLoginException::class)
    fun logIn(user: String, password: String, mfaToken: String, otp: String) {
        try {
            val response = identityApi.authenticate("urn:custom:mfa-otp", mfaToken, otp)
            onLogIn(response, user, password)
        } catch (error: RetrofitError) {
            throw ParticleLoginException(error)
        }
    }

    /**
     * Sign up with new account credentials to Particle cloud
     *
     * @param user     Required user name, must be a valid email address
     * @param password Required password
     */
    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun signUpWithUser(user: String, password: String) {
        signUpWithUser(SignUpInfo(user, password))
    }

    /**
     * Sign up with new account credentials to Particle cloud
     *
     * @param signUpInfo Required sign up information, must contain a valid email address and password
     */
    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun signUpWithUser(signUpInfo: SignUpInfo) {
        try {
            val response = identityApi.signUp(signUpInfo)
            val bodyString = String((response.body as TypedByteArray).bytes)
            val obj = JSONObject(bodyString)

            //workaround for sign up bug - invalid credentials bug
            if (obj.has("ok") && !obj.getBoolean("ok")) {
                val arrJson = obj.getJSONArray("errors")
                val arr = arrayOfNulls<String>(arrJson.length())

                for (i in 0 until arrJson.length()) {
                    arr[i] = arrJson.getString(i)
                }
                if (arr.isNotEmpty()) {
                    throw ParticleCloudException(Exception(arr[0]))
                }
            }
        } catch (error: RetrofitError) {
            throw ParticleCloudException(error)
        } catch (ignore: JSONException) {
            //ignore - who cares if we're not getting error response
        }

    }

    /**
     * Create new customer account on the Particle cloud and log in
     *
     * @param email     Required user name, must be a valid email address
     * @param password  Required password
     * @param productId Product id to use
     */
    @WorkerThread
    @Throws(ParticleLoginException::class)
    fun signUpAndLogInWithCustomer(email: String, password: String, productId: Int) {
        try {
            signUpAndLogInWithCustomer(SignUpInfo(email, password), productId)
        } catch (error: RetrofitError) {
            throw ParticleLoginException(error)
        }

    }

    /**
     * Create new customer account on the Particle cloud and log in
     *
     * @param signUpInfo Required sign up information, must contain a valid email address and password
     * @param productId  Product id to use
     */
    @WorkerThread
    @Throws(ParticleLoginException::class)
    fun signUpAndLogInWithCustomer(signUpInfo: SignUpInfo, productId: Int) {
        if (!all(signUpInfo.username, signUpInfo.password, productId)) {
            throw IllegalArgumentException(
                "Email, password, and product id must all be specified"
            )
        }

        signUpInfo.grantType = "client_credentials"
        try {
            val response = identityApi.signUpAndLogInWithCustomer(signUpInfo, productId)
            onLogIn(response, signUpInfo.username, signUpInfo.password)
        } catch (error: RetrofitError) {
            throw ParticleLoginException(error)
        }

    }


    /**
     * Create new customer account on the Particle cloud and log in
     *
     * @param email    Required user name, must be a valid email address
     * @param password Required password
     * @param orgSlug  Organization slug to use
     */
    @WorkerThread
    @Deprecated("Use product id or product slug instead")
    @Throws(ParticleCloudException::class)
    fun signUpAndLogInWithCustomer(email: String, password: String, orgSlug: String) {
        try {
            log.warn { "Use product id instead of organization slug." }
            @Suppress("DEPRECATION")
            signUpAndLogInWithCustomer(SignUpInfo(email, password), orgSlug)
        } catch (error: RetrofitError) {
            throw ParticleCloudException(error)
        }

    }

    /**
     * Create new customer account on the Particle cloud and log in
     *
     * @param signUpInfo Required sign up information, must contain a valid email address and password
     * @param orgSlug    Organization slug to use
     */
    @WorkerThread
    @Deprecated("Use product id or product slug instead")
    @Throws(ParticleCloudException::class)
    fun signUpAndLogInWithCustomer(signUpInfo: SignUpInfo, orgSlug: String) {
        if (!all(signUpInfo.username, signUpInfo.password, orgSlug)) {
            throw IllegalArgumentException(
                "Email, password, and organization must all be specified"
            )
        }

        signUpInfo.grantType = "client_credentials"
        try {
            @Suppress("DEPRECATION")
            val response = identityApi.signUpAndLogInWithCustomer(signUpInfo, orgSlug)
            onLogIn(response, signUpInfo.username, signUpInfo.password)
        } catch (error: RetrofitError) {
            throw ParticleCloudException(error)
        }
    }

    /**
     * Logout user, remove session data
     */
    fun logOut() {
        if (token != null) {
            token!!.cancelExpiration()
        }
        ParticleUser.removeSession()
        ParticleAccessToken.removeSession()
        token = null
        user = null
    }

    /** Get a list of instances of all user's claimed devices  */
    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun getDevices(): List<ParticleDevice> {
        log.info { "getDevices()" }
        return runHandlingCommonErrors {
            val apiDevices = mainApi.getDevices()
            appDataStorage.saveUserHasClaimedDevices(truthy(apiDevices))
            val result = apiDevices.map { getOfflineDevice(it) }
            pruneDeviceMap(apiDevices)
            result
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun userOwnsDevice(deviceId: String): Boolean {
        val idLower = deviceId.toLowerCase()
        return runHandlingCommonErrors {
            val devices = mainApi.getDevices()
            val firstMatch = devices.firstOrNull { idLower == it.deviceId.toLowerCase() }
            firstMatch != null
        }
    }

    /**
     * Get a specific device instance by its deviceID
     *
     * @param deviceID required deviceID
     * @return the device instance on success
     */
    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun getDevice(deviceID: String): ParticleDevice {
        log.info { "getDevice(): $deviceID" }
        val deviceCloudModel = runHandlingCommonErrors {
            mainApi.getDevice(deviceID)
        }

        val newDeviceState = fromCompleteDevice(deviceCloudModel)
        val device = getDeviceFromState(newDeviceState)
        updateDeviceState(
            device,
            newDeviceState,
            sendUpdateBroadcast = true,
            copyCompleteModelAttrsFromExistingState = false
        )
        return device
    }

    /**
     * Claim the specified device to the currently logged in user (without claim code mechanism)
     *
     * @param deviceID the deviceID
     */
    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun claimDevice(deviceID: String) {
        return runHandlingCommonErrors {
            mainApi.claimDevice(deviceID)
        }
    }

    /**
     * Get a short-lived claiming token for transmitting to soon-to-be-claimed device in
     * soft AP setup process
     *
     * @return a claim code string set on success (48 random bytes, base64 encoded
     * to 64 ASCII characters)
     */
    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun generateClaimCode(): Responses.ClaimCodeResponse {
        return runHandlingCommonErrors {
            // Offer empty string to appease newer OkHttp versions which require a POST body,
            // even if it's empty or (as far as the endpoint cares) nonsense
            mainApi.generateClaimCode("okhttp_appeasement")
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun generateClaimCode(productId: Int): Responses.ClaimCodeResponse {
        return runHandlingCommonErrors {
            // Offer empty string to appease newer OkHttp versions which require a POST body,
            // even if it's empty or (as far as the endpoint cares) nonsense
            mainApi.generateClaimCodeForOrg("okhttp_appeasement", productId)
        }
    }

    @WorkerThread
    @Deprecated("")
    @Throws(ParticleCloudException::class)
    fun generateClaimCodeForOrg(
        organizationSlug: String,
        productSlug: String
    ): Responses.ClaimCodeResponse {
        return runHandlingCommonErrors {
            log.warn { "Use product id instead of organization slug." }
            // Offer empty string to appease newer OkHttp versions which require a POST body,
            // even if it's empty or (as far as the endpoint cares) nonsense
            @Suppress("DEPRECATION")
            mainApi.generateClaimCodeForOrg(
                "okhttp_appeasement",
                organizationSlug,
                productSlug
            )
        }
    }

    // TODO: check if any javadoc has been added for this method in the iOS SDK
    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun requestPasswordReset(email: String) {
        try {
            identityApi.requestPasswordReset(email)
        } catch (error: RetrofitError) {
            throw ParticleCloudException(error)
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun getFirmwareUpdateInfo(
        platformId: Int,
        currentSystemFwVersion: String,
        currentNcpFwVersion: String?,
        currentNcpFwModuleVersion: Int?
    ): URL? {
        return runHandlingCommonErrors {
            val response = mainApi.getFirmwareUpdateInfo(
                platformId,
                currentSystemFwVersion,
                currentNcpFwVersion,
                currentNcpFwModuleVersion
            )

            // FIXME: see if this is correct...

            if (response == null) null else URL(response.nextFileUrl)
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun getPlatformId(serialNumber: String): ParticleDeviceType {
        return runHandlingCommonErrors {
            val idsResponse = mainApi.getDeviceIdentifiers(serialNumber)
            ParticleDeviceType.fromInt(idsResponse.platformId)
        }
    }
    //endregion


    //region mesh APIs
    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun getNetworks(): List<ParticleNetwork> {
        return runHandlingCommonErrors {
            // FIXME: implement paging!
            // FIXME: implement caching! (including for the networks map!)
            // Make page info a param here?  Where do we expose this?
            val networkDatas = mainApi.getNetworks()
            networkDatas.map { ParticleNetwork(it) }
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun getNetwork(networkId: String): ParticleNetwork {
        return runHandlingCommonErrors {
            val networkData = mainApi.getNetwork(networkId)
            ParticleNetwork(networkData)
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun getNetworkDevices(networkId: String): List<DeviceMeshMembership> {
        return runHandlingCommonErrors {
            mainApi.getNetworkDevices(networkId)
        }
    }

    // FIXME: this is not great API.
    @WorkerThread
    @Throws(ParticleCloudException::class)
    @JvmOverloads
    fun registerMeshNetwork(
        gatewayDeviceId: String,
        networkType: ParticleNetworkType,
        networkName: String,
        iccId: String? = null
    ): RegisteredNetwork {


        // FIXME: handle error cases
        //        400 Invalid request
        //        {
        //            Error: `invalid name`
        //        }
        //        402 Payment required
        //        {
        //            Error: `mesh networks above quota, payment needed`
        //        }

        return runHandlingCommonErrors {
            val response = if (iccId == null) {
                mainApi.registerMeshNetwork(gatewayDeviceId, networkType, networkName)
            } else {
                mainApi.registerMeshNetwork(gatewayDeviceId, networkType, networkName, iccId)
            }

            response.network
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun addDeviceToMeshNetwork(deviceId: String, networkId: String) {
        modifyMeshNetwork(deviceId, "add-device", networkId)
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun removeDeviceFromAnyMeshNetwork(deviceId: String) {
        return runHandlingCommonErrors {
            mainApi.removeDeviceFromAnyNetwork(deviceId)
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun enableGatewayOnMeshNetwork(deviceId: String, networkId: String) {
        modifyMeshNetwork(deviceId, "gateway-enable", networkId)
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun disableGatewayOnMeshNetwork(deviceId: String, networkId: String) {
        modifyMeshNetwork(deviceId, "gateway-disable", networkId)
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun getFullMobileSecret(
        serialNumber: String,
        partialMobileSecret: String
    ): MobileSecretResponse {
        return runHandlingCommonErrors {
            mainApi.getFullMobileSecret(serialNumber, partialMobileSecret)
        }
    }
    //endregion


    //region billing/pricing
    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun getPaymentCard(): CardOnFileResponse {
        return runHandlingCommonErrors {
            mainApi.getPaymentCard()
        }
    }
    //endregion

    //region SIM registration
    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun checkSim(iccId: String): Pair<ParticleSimStatus, String> {
        val code = try {
            val response = mainApi.checkSim(iccId)
            response.status
        } catch (ex: RetrofitError) {
            if (ex.kind != Kind.HTTP) {
                throw ParticleCloudException(ex)
            }
            ex.response.status
        }
        return statusCodeToSimStatus(code)
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun activateSim(iccId: String): Response {
        return runHandlingCommonErrors {
            mainApi.takeActionOnSim(iccId, "activate")
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun deactivateSim(iccId: String): Response {
        return runHandlingCommonErrors {
            mainApi.takeActionOnSim(iccId, "deactivate")
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun reactivateSim(iccId: String): Response {
        return runHandlingCommonErrors {
            mainApi.takeActionOnSim(iccId, "reactivate")
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun unpauseSim(iccId: String, limitInMBsForUnpause: Int): Response {
        return runHandlingCommonErrors {
            mainApi.takeActionOnSim(iccId, "reactivate", limitInMBsForUnpause)
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun setDataLimit(iccId: String, limitInMBs: Int): Response {
        return runHandlingCommonErrors {
            mainApi.setDataLimit(iccId, limitInMBs)
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun getSim(iccId: String): ParticleSim {
        return runHandlingCommonErrors {
            mainApi.getSim(iccId)
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun getPricingImpact(
        action: PricingImpactAction,
        networkType: PricingImpactNetworkType,
        deviceId: String? = null,
        networkId: String? = null,
        iccid: String? = null
    ): ParticlePricingInfo {
        return runHandlingCommonErrors {
            mainApi.getPricingImpact(
                action.apiString,
                networkType.apiString,
                deviceId,
                networkId,
                iccid
            )
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun getServiceAgreements(): UserServiceAgreementsResponse {
        return runHandlingCommonErrors {
            mainApi.getServiceAgreements()
        }
    }
    //endregion


    //region Events pub/sub methods

    /**
     * Subscribe to events from one specific device. If the API user has the device claimed, then
     * she will receive all events, public and private, published by that device.  If the API user
     * does not own the device she will only receive public events.
     *
     * @param eventName       The name for the event
     * @param event           A JSON-formatted string to use as the event payload
     * @param eventVisibility An IntDef "enum" determining the visibility of the event
     * @param timeToLive      TTL, or Time To Live: a piece of event metadata representing the
     * number of seconds that the event data is still considered relevant.
     * After the TTL has passed, event listeners should consider the
     * information stale or out of date.
     * e.g.: an outdoor temperature reading might have a TTL of somewhere
     * between 600 (10 minutes) and 1800 (30 minutes).  The geolocation of a
     * large piece of farm equipment which remains stationary most of the
     * time but may be moved to a different field once in a while might
     * have a TTL of 86400 (24 hours).
     */
    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun publishEvent(
        eventName: String,
        event: String?,
        @ParticleEventVisibility eventVisibility: Int,
        timeToLive: Int = 60
    ) {
        eventsDelegate.publishEvent(eventName, event, eventVisibility, timeToLive)
    }

    /**
     * NOTE: This method will be deprecated in the future. Please use
     * [.subscribeToMyDevicesEvents] instead.
     *
     *
     * Subscribe to the *firehose* of public events, plpus all private events published by
     * the devices the API user owns.
     *
     * @param eventNamePrefix A string to filter on for events.  If null, all events will be matched.
     * @param handler         The ParticleEventHandler to receive the events
     * @return a unique subscription ID for the eventListener that's been registered.  This ID is
     * used to unsubscribe this event listener later.
     */
    @Deprecated(
        "This method will be removed in a future revision of the SDK.  " +
                "Please use .subscribeToMyDevicesEvents() instead"
    )
    @WorkerThread
    @Throws(IOException::class)
    fun subscribeToAllEvents(eventNamePrefix: String?, handler: ParticleEventHandler): Long {
        Log.w(
            "ParticleCloud",
            "This method will be deprecated in the future. " + "Please use subscribeToMyDevicesEvents() instead."
        )
        return eventsDelegate.subscribeToAllEvents(eventNamePrefix, handler)
    }

    /**
     * Subscribe to all events, public and private, published by devices owned by the logged-in account.
     *
     *
     * see [.subscribeToAllEvents] for info on the
     * arguments and return value.
     */
    @WorkerThread
    @Throws(IOException::class)
    fun subscribeToMyDevicesEvents(eventNamePrefix: String?, handler: ParticleEventHandler): Long {
        Log.d("ParticleCloud", "subscribeToMyDevicesEvents(), prefix=$eventNamePrefix")
        return eventsDelegate.subscribeToMyDevicesEvents(eventNamePrefix, handler)
    }

    /**
     * Subscribe to events from a specific device.
     *
     *
     * If the API user has claimed the device, then she will receive all events, public and private,
     * published by this device.  If the API user does *not* own the device, she will only
     * receive public events.
     *
     * @param deviceID the device to listen to events from
     *
     *
     * see [.subscribeToAllEvents] for info on the
     * arguments and return value.
     */
    @WorkerThread
    @Throws(IOException::class)
    fun subscribeToDeviceEvents(
        eventNamePrefix: String?,
        deviceID: String,
        eventHandler: ParticleEventHandler
    ): Long {
        return eventsDelegate.subscribeToDeviceEvents(eventNamePrefix, deviceID, eventHandler)
    }

    /**
     * Unsubscribe event listener from events.
     *
     * @param eventListenerID The ID of the event listener you want to unsubscribe from events
     */
    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun unsubscribeFromEventWithID(eventListenerID: Long) {
        log.trace { "Unsubscribing from events where eventListenerID=$eventListenerID" }
        eventsDelegate.unsubscribeFromEventWithID(eventListenerID)
    }

    /**
     * Unsubscribe event listener from events.
     *
     * @param handler Particle event listener you want to unsubscribe from events
     */
    @WorkerThread
    @Throws(ParticleCloudException::class)
    internal fun unsubscribeFromEventWithHandler(handler: SimpleParticleEventHandler) {
        eventsDelegate.unsubscribeFromEventWithHandler(handler)
    }
    //endregion


    //region package-only API
    @WorkerThread
    internal fun unclaimDevice(deviceId: String) {
        mainApi.unclaimDevice(deviceId)
        synchronized(devices) {
            devices.remove(deviceId)
        }
        sendUpdateBroadcast()
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    internal fun rename(deviceId: String, newName: String) {
        val particleDevice: ParticleDevice = synchronized(devices) {
            devices[deviceId]!!
        }
        val originalDeviceState = particleDevice.deviceState

        val stateWithNewName = originalDeviceState.copy(name = newName)
        updateDeviceState(particleDevice, stateWithNewName, true)
        try {
            mainApi.nameDevice(originalDeviceState.deviceId, newName)
        } catch (e: RetrofitError) {
            // oops, change the name back.
            updateDeviceState(particleDevice, originalDeviceState, true)
            throw ParticleCloudException(e)
        }

    }

    @Deprecated("")
    @WorkerThread
    @Throws(ParticleCloudException::class)
    internal fun changeDeviceName(deviceId: String, newName: String) {
        rename(deviceId, newName)
    }

    @WorkerThread
    internal fun onDeviceNotConnected(deviceState: DeviceState) {
        // Called when a cloud API call receives a result in which the "coreInfo.connected" is false
        val newState = deviceState.copy(isConnected = false)
        val device = getDeviceFromState(newState)
        updateDeviceState(device, newState, true)
    }

    // FIXME: exposing this is weak, figure out something better
    internal fun notifyDeviceChanged() {
        sendUpdateBroadcast()
    }

    internal fun sendSystemEventBroadcast(stateChange: DeviceStateChange) {
        val intent = Intent(BroadcastContract.BROADCAST_SYSTEM_EVENT)
        intent.putExtra("event", stateChange)
        broadcastManager.sendBroadcast(intent)
    }

    // this is accessible at the package level for access from ParticleDevice's Parcelable impl
    internal fun getDeviceFromState(deviceState: DeviceState): ParticleDevice {
        synchronized(devices) {
            return if (devices.containsKey(deviceState.deviceId)) {
                devices[deviceState.deviceId]!!
            } else {
                val device = ParticleDevice(mainApi, this, deviceState)
                devices[deviceState.deviceId] = device
                device
            }
        }
    }
    //endregion


    //region private API
    private fun getOfflineDevice(offlineDevice: CompleteDevice): ParticleDevice {
        val newDeviceState = fromCompleteDevice(offlineDevice)
        val device = getDeviceFromState(newDeviceState)
        updateDeviceState(device, newDeviceState, false)
        return device
    }

    private fun updateDeviceState(
        device: ParticleDevice,
        newState: DeviceState,
        sendUpdateBroadcast: Boolean,
        copyCompleteModelAttrsFromExistingState: Boolean = true
    ) {
        val actualNewState = if (!copyCompleteModelAttrsFromExistingState) {
            newState
        } else {
            val mobileSecret = newState.mobileSecret ?: device.deviceState.mobileSecret

            val functions = if (newState.functions.isNullOrEmpty()) {
                device.deviceState.functions
            } else {
                newState.functions
            }

            val variables = if (newState.variables.isNullOrEmpty()) {
                device.deviceState.variables
            } else {
                newState.variables
            }

            newState.copy(
                mobileSecret = mobileSecret,
                functions = functions,
                variables = variables
            )
        }

        device.deviceState = actualNewState
        if (sendUpdateBroadcast) {
            sendUpdateBroadcast()
        }
    }

    private fun sendUpdateBroadcast() {
        log.info { "sendUpdateBroadcast()" }
        broadcastManager.sendBroadcast(Intent(BroadcastContract.BROADCAST_DEVICES_UPDATED))
    }

    private fun onLogIn(response: Responses.LogInResponse, user: String, password: String) {
        ParticleAccessToken.removeSession()
        this.token = ParticleAccessToken.fromNewSession(response)
        this.token!!.delegate = tokenDelegate
        this.user = ParticleUser.fromNewCredentials(user, password)
    }

    private fun fromCompleteDevice(completeDevice: CompleteDevice): DeviceState {
        // FIXME: we're sometimes getting back nulls in the list of functions...  WUT?
        // Once analytics are in place, look into adding something here so we know where
        // this is coming from.  In the meantime, filter out nulls from this list, since that's
        // obviously doubleplusungood.
        val functions = completeDevice.functions?.filterNotNull()?.toSet() ?: setOf()
        val variables = transformVariables(completeDevice)

        return DeviceState(
            deviceId = completeDevice.deviceId,
            name = completeDevice.name,
            isConnected = completeDevice.isConnected,
            cellular = completeDevice.cellular,
            deviceType = ParticleDeviceType.fromInt(completeDevice.productId),
            platformId = completeDevice.platformId,
            productId = completeDevice.productId,
            imei = completeDevice.imei,
            lastIccid = completeDevice.lastIccid,
            currentBuild = completeDevice.currentBuild,
            defaultBuild = completeDevice.defaultBuild,
            functions = functions,
            variables = variables,
            ipAddress = completeDevice.ipAddress,
            lastAppName = completeDevice.lastAppName,
            status = completeDevice.status,
            lastHeard = completeDevice.lastHeard,
            serialNumber = completeDevice.serialNumber,
            mobileSecret = completeDevice.mobileSecret,
            iccid = completeDevice.iccid,
            systemFirmwareVersion = completeDevice.systemFirmwareVersion,
            notes = completeDevice.notes
        )
    }

    private fun pruneDeviceMap(latestCloudDeviceList: List<CompleteDevice>) {
        synchronized(devices) {
            // make a copy of the current keyset since we mutate `devices` below
            val currentDeviceIds = devices.keys.toSet()
            val newDeviceIds = latestCloudDeviceList.map { it.deviceId }.toSet()
            // The resulting set, "toRemove" is all the device IDs which we have in our devices map,
            // but which we did not hear about in this latest update from the cloud
            val toRemove = currentDeviceIds.minus(newDeviceIds)
            for (deviceId in toRemove) {
                devices.remove(deviceId)
            }
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    private fun refreshAccessToken(refreshToken: String) {
        return runHandlingCommonErrors {
            val response = identityApi.logIn("refresh_token", refreshToken)
            ParticleAccessToken.removeSession()
            this.token = ParticleAccessToken.fromNewSession(response)
            this.token!!.delegate = tokenDelegate
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    private fun <T> runHandlingCommonErrors(toRun: () -> T): T {
        try {
            return toRun()

        } catch (error: RetrofitError) {
            throw ParticleCloudException(error)

        } catch (e: MalformedURLException) {
            throw ParticleCloudException(e)
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    private fun modifyMeshNetwork(deviceId: String, action: String, networkId: String): Response {
        return runHandlingCommonErrors {
            mainApi.modifyMeshNetwork(
                networkId,
                MeshNetworkChange(action, deviceId)
            )
        }
    }


    private inner class TokenDelegate : ParticleAccessToken.ParticleAccessTokenDelegate {

        override fun accessTokenExpiredAt(accessToken: ParticleAccessToken, expirationDate: Date) {
            // handle auto-renewal of expired access tokens by internal timer event
            val refreshToken = accessToken.refreshToken
            if (refreshToken != null) {
                try {
                    refreshAccessToken(refreshToken)
                    return
                } catch (e: ParticleCloudException) {
                    log.error("Error while trying to refresh token: ", e)
                }

            }

            ParticleAccessToken.removeSession()
            token = null
        }
    }


    companion object {

        private val log = KotlinLogging.logger {}

        /**
         * Singleton instance of ParticleCloud class
         *
         * @return ParticleCloud
         */
        @Deprecated(
            "use {@link ParticleCloudSDK#getCloud()} instead.  This interface will be removed\n" +
                    "      some time before the 1.0 release."
        )
        @Synchronized
        @JvmStatic
        operator fun get(context: Context): ParticleCloud {
            log.warn {
                "ParticleCloud.get() is deprecated and will be removed before the 1.0 release. " +
                        "Use ParticleCloudSDK.getCloud() instead!"
            }
            if (!ParticleCloudSDK.isInitialized()) {
                ParticleCloudSDK.init(context)
            }
            return ParticleCloudSDK.getCloud()
        }


        private fun transformVariables(completeDevice: CompleteDevice): Map<String, VariableType> {
            if (completeDevice.variables == null) {
                return arrayMapOf()
            }

            val variables = ArrayMap<String, VariableType>()

            for ((key, value) in completeDevice.variables) {
                if (!all(key, value)) {
                    log.warn {
                        "Found null key and/or value for variable in device " +
                                "${completeDevice.name}.  key=$key, value=$value"
                    }
                    continue
                }

                fun String.toVariableType(): VariableType? {
                    return when (this) {
                        "int32" -> VariableType.INT
                        "double" -> VariableType.DOUBLE
                        "string" -> VariableType.STRING
                        "bool" -> VariableType.BOOLEAN
                        else -> null
                    }
                }

                val variableType = value.toVariableType()
                if (variableType == null) {
                    log.warn { "Unknown variable type for device ${completeDevice.name}: '$key'" }
                    continue
                }

                variables[key] = variableType
            }

            return variables
        }
    }
}
