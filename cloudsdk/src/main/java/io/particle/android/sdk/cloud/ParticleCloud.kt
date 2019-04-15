package io.particle.android.sdk.cloud

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.collection.ArrayMap
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.cloud.ParticleDevice.VariableType
import io.particle.android.sdk.cloud.Responses.MeshNetworkRegistrationResponse.RegisteredNetwork
import io.particle.android.sdk.cloud.Responses.Models
import io.particle.android.sdk.cloud.Responses.Models.CompleteDevice
import io.particle.android.sdk.cloud.Responses.Models.SimpleDevice
import io.particle.android.sdk.cloud.exceptions.PartialDeviceListResultException
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.android.sdk.cloud.exceptions.ParticleLoginException
import io.particle.android.sdk.cloud.models.*
import io.particle.android.sdk.persistance.AppDataStorage
import io.particle.android.sdk.utils.Funcy
import io.particle.android.sdk.utils.Py.all
import io.particle.android.sdk.utils.Py.list
import io.particle.android.sdk.utils.Py.set
import io.particle.android.sdk.utils.Py.truthy
import io.particle.android.sdk.utils.TLog
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


// FIXME: move device state management out to another class
// FIXME: move some of the type conversion junk out of this into another class, too


class ParticleCloud internal constructor(
    schemeAndHostname: Uri,
    private val mainApi: ApiDefs.CloudApi,
    private val identityApi: ApiDefs.IdentityApi,
    // FIXME: document why this exists (and try to make it not exist...)
    private val deviceFastTimeoutApi: ApiDefs.CloudApi,
    private val appDataStorage: AppDataStorage,
    private val broadcastManager: LocalBroadcastManager,
    gson: Gson, executor: ExecutorService
) {
    private val tokenDelegate = TokenDelegate()
    private val eventsDelegate: EventsDelegate
    private val parallelDeviceFetcher: ParallelDeviceFetcher

    private val devices = ArrayMap<String, ParticleDevice>()
    private val networks = ArrayMap<String, ParticleNetwork>()

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
        this.parallelDeviceFetcher = ParallelDeviceFetcher.newFetcherUsingExecutor(executor)
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
            log.w("Use product id instead of organization slug.")
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
        return runHandlingCommonErrors {
            val simpleDevices = mainApi.getDevices()

            appDataStorage.saveUserHasClaimedDevices(truthy(simpleDevices))

            val result = list<ParticleDevice>()

            for (simpleDevice in simpleDevices) {
                val device: ParticleDevice





                // FIXME: TEST ONLY, REMOVE ME


//                if (simpleDevice.isConnected) {
//                    device = getDevice(simpleDevice.id, false)
//                } else {
                    device = getOfflineDevice(simpleDevice)
//                }






                result.add(device)
            }

            pruneDeviceMap(simpleDevices)

            result
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun userOwnsDevice(deviceId: String): Boolean {
        val idLower = deviceId.toLowerCase()
        return runHandlingCommonErrors {
            val devices = mainApi.getDevices()
            val firstMatch = Funcy.findFirstMatch(
                devices
            ) { testTarget -> idLower == testTarget.id.toLowerCase() }
            firstMatch != null
        }
    }

    // FIXME: devise a less temporary way to expose this method
    // FIXME: stop the duplication that's happening here
    // FIXME: ...think harder about this whole thing.  This is unique in that it's the only
    // operation that could _partially_ succeed.
    // FIXME: make this internal!
    @WorkerThread
    @Throws(PartialDeviceListResultException::class, ParticleCloudException::class)
    fun getDevicesParallel(useShortTimeout: Boolean): List<ParticleDevice> {
        val simpleDevices: List<Models.SimpleDevice>
        try {
            simpleDevices = mainApi.getDevices()
            appDataStorage.saveUserHasClaimedDevices(truthy(simpleDevices))


            // divide up into online and offline
            val offlineDevices = list<Models.SimpleDevice>()
            val onlineDevices = list<Models.SimpleDevice>()

            for (simpleDevice in simpleDevices) {
                val targetList = if (simpleDevice.isConnected)
                    onlineDevices
                else
                    offlineDevices
                targetList.add(simpleDevice)
            }


            val result = list<ParticleDevice>()

            // handle the offline devices
            for (offlineDevice in offlineDevices) {
                result.add(getOfflineDevice(offlineDevice))
            }


            // handle the online devices
            val apiToUse = if (useShortTimeout)
                deviceFastTimeoutApi
            else
                mainApi
            // FIXME: don't hardcode this here
            val timeoutInSecs = if (useShortTimeout) 5 else 35
            val results = parallelDeviceFetcher.fetchDevicesInParallel(
                onlineDevices, apiToUse, timeoutInSecs
            )

            // FIXME: make this logic more elegant
            var shouldThrowIncompleteException = false
            for (fetchResult in results) {
                // fetchResult shouldn't be null, but...
                // FIXME: eliminate this ambiguity ^^^, it's either possible that it's null, or it isn't.
                if (fetchResult?.fetchedDevice == null) {
                    shouldThrowIncompleteException = true
                } else {
                    result.add(getDevice(fetchResult.fetchedDevice, false))
                }
            }

            pruneDeviceMap(simpleDevices)

            if (shouldThrowIncompleteException) {
                throw PartialDeviceListResultException(result)
            }

            return result

        } catch (error: RetrofitError) {
            throw ParticleCloudException(error)
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

        log.w("Called getDevice($deviceID)")

        return getDevice(deviceID, true)
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
            log.w("Use product id instead of organization slug.")
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
    fun requestPasswordResetForCustomer(email: String, productId: Int) {
        return runHandlingCommonErrors {
            identityApi.requestPasswordResetForCustomer(email, productId)
        }
    }

    @WorkerThread
    @Deprecated("")
    @Throws(ParticleCloudException::class)
    fun requestPasswordResetForCustomer(email: String, organizationSlug: String) {
        return runHandlingCommonErrors {
            log.w("Use product id instead of organization slug.")
            @Suppress("DEPRECATION")
            identityApi.requestPasswordResetForCustomer(email, organizationSlug)
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
            ParticleNetwork(networkData)        }

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
    private fun modifyMeshNetwork(deviceId: String, action: String, networkId: String): Response {
        return runHandlingCommonErrors {
            mainApi.modifyMeshNetwork(
                networkId,
                MeshNetworkChange(action, deviceId)
            )
        }
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
     * Subscribe to the *firehose* of public events, plus all private events published by
     * the devices the API user owns.
     *
     * @param eventNamePrefix A string to filter on for events.  If null, all events will be matched.
     * @param handler         The ParticleEventHandler to receive the events
     * @return a unique subscription ID for the eventListener that's been registered.  This ID is
     * used to unsubscribe this event listener later.
     */
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

        val stateWithNewName = DeviceState.withNewName(originalDeviceState, newName)
        updateDeviceState(stateWithNewName, true)
        try {
            mainApi.nameDevice(originalDeviceState.deviceId, newName)
        } catch (e: RetrofitError) {
            // oops, change the name back.
            updateDeviceState(originalDeviceState, true)
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
        val newState = DeviceState.withNewConnectedState(deviceState, false)
        updateDeviceState(newState, true)
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
    @WorkerThread
    @Throws(ParticleCloudException::class)
    private fun getDevice(deviceID: String, sendUpdate: Boolean): ParticleDevice {
        log.w("Called PRIVATE getDevice($deviceID)")

        val deviceCloudModel = runHandlingCommonErrors {
            mainApi.getDevice(deviceID)
        }

        return getDevice(deviceCloudModel, sendUpdate)
    }

    private fun getDevice(deviceModel: CompleteDevice, sendUpdate: Boolean): ParticleDevice {
        val newDeviceState = fromCompleteDevice(deviceModel)
        val device = getDeviceFromState(newDeviceState)
        updateDeviceState(newDeviceState, sendUpdate)
        return device
    }

    private fun getOfflineDevice(offlineDevice: Models.SimpleDevice): ParticleDevice {
        val newDeviceState = fromSimpleDeviceModel(offlineDevice)
        val device = getDeviceFromState(newDeviceState)
        updateDeviceState(newDeviceState, false)
        return device
    }

    private fun updateDeviceState(newState: DeviceState, sendUpdateBroadcast: Boolean) {
        val device = getDeviceFromState(newState)
        device.deviceState = newState
        if (sendUpdateBroadcast) {
            sendUpdateBroadcast()
        }
    }

    private fun sendUpdateBroadcast() {
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
        val functions = set(Funcy.filter(completeDevice.functions, Funcy.notNull()))
        val variables = transformVariables(completeDevice)

        return DeviceState.DeviceStateBuilder(completeDevice.deviceId, functions, variables)
            .name(completeDevice.name)
            .cellular(completeDevice.cellular)
            .connected(completeDevice.isConnected)
            .version(completeDevice.version)
            .deviceType(ParticleDeviceType.fromInt(completeDevice.productId))
            .platformId(completeDevice.platformId)
            .productId(completeDevice.productId)
            .imei(completeDevice.imei)
            .iccid(completeDevice.lastIccid)
            .currentBuild(completeDevice.currentBuild)
            .defaultBuild(completeDevice.defaultBuild)
            .ipAddress(completeDevice.ipAddress)
            .lastAppName(completeDevice.lastAppName)
            .status(completeDevice.status)
            .requiresUpdate(completeDevice.requiresUpdate)
            .lastHeard(completeDevice.lastHeard)
            .serialNumber(completeDevice.serialNumber)
            .mobileSecret(completeDevice.mobileSecret)
            .build()
    }

    // for offline devices
    private fun fromSimpleDeviceModel(offlineDevice: Models.SimpleDevice): DeviceState {
        val functions = HashSet<String>()
        val variables = ArrayMap<String, VariableType>()

        return DeviceState.DeviceStateBuilder(offlineDevice.id, functions, variables)
            .name(offlineDevice.name)
            .cellular(offlineDevice.cellular)
            .connected(offlineDevice.isConnected)
            .version("")
            .deviceType(ParticleDeviceType.fromInt(offlineDevice.productId))
            .platformId(offlineDevice.platformId)
            .productId(offlineDevice.productId)
            .imei(offlineDevice.imei)
            .iccid(offlineDevice.lastIccid)
            .currentBuild(offlineDevice.currentBuild)
            .defaultBuild(offlineDevice.defaultBuild)
            .ipAddress(offlineDevice.ipAddress)
            .lastAppName("")
            .status(offlineDevice.status)
            .requiresUpdate(false)
            .lastHeard(offlineDevice.lastHeard)
            .build()
    }


    private fun pruneDeviceMap(latestCloudDeviceList: List<SimpleDevice>) {
        synchronized(devices) {
            // make a copy of the current keyset since we mutate `devices` below
            val currentDeviceIds = set(devices.keys)
            val newDeviceIds = set(latestCloudDeviceList.map { it.id })
            // quoting the Sets docs for this next operation:
            // "The returned set contains all elements that are contained by set1 and
            //  not contained by set2"
            // In short, this set is all the device IDs which we have in our devices map,
            // but which we did not hear about in this latest update from the cloud
            val toRemove = currentDeviceIds.getDifference(newDeviceIds)
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


    private inner class TokenDelegate : ParticleAccessToken.ParticleAccessTokenDelegate {

        override fun accessTokenExpiredAt(accessToken: ParticleAccessToken, expirationDate: Date) {
            // handle auto-renewal of expired access tokens by internal timer event
            val refreshToken = accessToken.refreshToken
            if (refreshToken != null) {
                try {
                    refreshAccessToken(refreshToken)
                    return
                } catch (e: ParticleCloudException) {
                    log.e("Error while trying to refresh token: ", e)
                }

            }

            ParticleAccessToken.removeSession()
            token = null
        }
    }

    companion object {

        private val log = TLog.get(ParticleCloud::class.java)

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
            log.w(
                "ParticleCloud.get() is deprecated and will be removed before the 1.0 release. " +
                        "Use ParticleCloudSDK.getCloud() instead!"
            )
            if (!ParticleCloudSDK.isInitialized()) {
                ParticleCloudSDK.init(context)
            }
            return ParticleCloudSDK.getCloud()
        }


        private fun transformVariables(completeDevice: CompleteDevice): Map<String, VariableType> {
            if (completeDevice.variables == null) {
                return emptyMap()
            }

            val variables = ArrayMap<String, VariableType>()

            for ((key, value) in completeDevice.variables) {
                if (!all(key, value)) {
                    log.w(
                        "Found null key and/or value for variable in device " +
                                "${completeDevice.name}.  key=$key, value=$value"
                    )
                    continue
                }

                fun String.toVariableType(): VariableType? {
                    return when (this) {
                        "int32" -> VariableType.INT
                        "double" -> VariableType.DOUBLE
                        "string" -> VariableType.STRING
                        else -> null
                    }
                }

                val variableType = value.toVariableType()
                if (variableType == null) {
                    log.w("Unknown variable type for device ${completeDevice.name}: '$key'")
                    continue
                }

                variables[key] = variableType
            }

            return variables
        }
    }
}
