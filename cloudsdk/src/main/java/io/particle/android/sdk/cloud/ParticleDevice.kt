package io.particle.android.sdk.cloud

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.WorkerThread
import io.particle.android.sdk.cloud.Responses.ReadDoubleVariableResponse
import io.particle.android.sdk.cloud.Responses.ReadIntVariableResponse
import io.particle.android.sdk.cloud.Responses.ReadObjectVariableResponse
import io.particle.android.sdk.cloud.Responses.ReadStringVariableResponse
import io.particle.android.sdk.cloud.Responses.ReadVariableResponse
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.android.sdk.cloud.models.DeviceStateChange
import io.particle.android.sdk.utils.Preconditions
import io.particle.android.sdk.utils.Py.list
import io.particle.android.sdk.utils.TLog
import io.particle.android.sdk.utils.buildIntValueMap
import io.particle.android.sdk.utils.join
import okio.buffer
import okio.source
import org.greenrobot.eventbus.EventBus
import org.json.JSONException
import org.json.JSONObject
import retrofit.RetrofitError
import retrofit.mime.TypedByteArray
import retrofit.mime.TypedFile
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList


// don't warn about public APIs not being referenced inside this module, or about
// the _default locale_ in a bunch of backend code
@SuppressLint("DefaultLocale")
class ParticleDevice internal constructor(
    private val mainApi: ApiDefs.CloudApi,
    val cloud: ParticleCloud,
    @field:Volatile internal var deviceState: DeviceState
) : Parcelable {

    private val subscriptions = CopyOnWriteArrayList<Long>()

    @Volatile
    var isFlashing = false
        private set

    /**
     * Device ID string
     */
    val id: String
        get() = deviceState.deviceId

    /**
     * Device name. Device can be renamed in the cloud via #setName(String)
     */
    /**
     * Rename the device in the cloud. If renaming fails name will stay the same.
     */
    var name: String
        get() { return deviceState.name ?: "" }

        @WorkerThread
        @Throws(ParticleCloudException::class)
        set(newName) {
            cloud.rename(this.deviceState.deviceId, newName)
        }

    /**
     * Is device connected to the cloud?
     */
    val isConnected: Boolean
        get() { return deviceState.isConnected ?: false }

    /**
     * Get an immutable set of all the function names exposed by device
     */
    // no need for a defensive copy, this is an immutable set
    val functions: Set<String>
        get() { return deviceState.functions }

    /**
     * Get an immutable map of exposed variables on device with their respective types.
     */
    // no need for a defensive copy, this is an immutable set
    val variables: Map<String, VariableType>
        get() { return deviceState.variables }

    /** Device firmware version string */
    val version: String?
        get() { return deviceState.systemFirmwareVersion }

    val deviceType: ParticleDeviceType?
        get() { return deviceState.deviceType }

    val platformID: Int?
        get() { return deviceState.platformId }

    val productID: Int?
        get() { return deviceState.productId }

    val isCellular: Boolean?
        get() { return deviceState.cellular }

    val imei: String?
        get() { return deviceState.imei }

    val lastIccid: String?
        get() { return deviceState.lastIccid }

    val iccid: String?
        get() { return deviceState.iccid }

    val currentBuild: String?
        get() { return deviceState.currentBuild }

    val defaultBuild: String?
        get() { return deviceState.defaultBuild }

    val ipAddress: String?
        get() { return deviceState.ipAddress }

    val lastAppName: String?
        get() { return deviceState.lastAppName }

    val status: String?
        get() { return deviceState.status }

    val lastHeard: Date?
        get() { return deviceState.lastHeard }

    val serialNumber: String?
        get() { return deviceState.serialNumber }

    val mobileSecret: String?
        get() { return deviceState.mobileSecret }

    var notes: String?
        get() { return deviceState.notes }
        @WorkerThread
        @Throws(ParticleCloudException::class)
        set(newNote) {
            try {
                mainApi.setDeviceNote(id, newNote ?: "")
            } catch (e: RetrofitError) {
                throw ParticleCloudException(e)
            }
        }

    val isRunningTinker: Boolean
        get() {
            val lowercaseFunctions = list<String>()
            for (func in deviceState.functions) {
                lowercaseFunctions.add(func.toLowerCase())
            }
            val tinkerFunctions = list("analogread", "analogwrite", "digitalread", "digitalwrite")
            return isConnected && lowercaseFunctions.containsAll(tinkerFunctions)
        }

    // included for Java backwards compat: the method was named getID(),
    // and the new Kotlin property generates a method named getId()
    @Deprecated(
        message = "Use #id property / #getId() instead",
        replaceWith = ReplaceWith(
            "getId()",
            "io.particle.android.sdk.cloud.ParticleDevice"
        )
    )
    fun getID(): String {
        return id
    }

    enum class ParticleDeviceType(val intValue: Int) {
        OTHER(Integer.MIN_VALUE),
        CORE(0),
        PHOTON(6),
        P1(8),
        RASPBERRY_PI(31),
        RED_BEAR_DUO(88),
        BLUZ(103),
        DIGISTUMP_OAK(82),
        ELECTRON(10),
        ESP32(11),
        ARGON(12),
        BORON(13),
        XENON(14),
        A_SOM(22),
        B_SOM(23),
        X_SOM(24),
        B5_SOM(25);


        companion object {

            private val intValueMap = buildIntValueMap(values()) { state -> state.intValue }

            @JvmStatic
            fun fromInt(intValue: Int): ParticleDeviceType {
                return intValueMap.get(intValue, ParticleDeviceType.OTHER)
            }
        }
    }

    enum class ParticleDeviceState {
        CAME_ONLINE,
        FLASH_STARTED,
        FLASH_SUCCEEDED,
        FLASH_FAILED,
        APP_HASH_UPDATED,
        ENTERED_SAFE_MODE,
        SAFE_MODE_UPDATER,
        WENT_OFFLINE,
        UNKNOWN
    }

    enum class VariableType {
        INT,
        DOUBLE,
        STRING,
        BOOLEAN
    }


    class FunctionDoesNotExistException(functionName: String) :
        Exception("Function $functionName does not exist on this device")


    class VariableDoesNotExistException(variableName: String) :
        Exception("Variable $variableName does not exist on this device")


    enum class KnownApp constructor(val appName: String) {
        TINKER("tinker")
    }

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Deprecated: field no longer available from the Particle devices API")
    fun requiresUpdate(): Boolean {
        return false
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun getCurrentDataUsage(): Float {
        // FIXME: create a proper GSON model for this.
        var maxUsage = 0f
        try {
            val response = mainApi.getCurrentDataUsage(deviceState.lastIccid!!)
            val result = JSONObject(String((response.body as TypedByteArray).bytes))
            val usages = result.getJSONArray("usage_by_day")

            for (i in 0 until usages.length()) {
                val usageElement = usages.getJSONObject(i)
                if (usageElement.has("mbs_used_cumulative")) {
                    val usage = usageElement.getDouble("mbs_used_cumulative")
                    if (usage > maxUsage) {
                        maxUsage = usage.toFloat()
                    }
                }
            }
        } catch (e: JSONException) {
            throw ParticleCloudException(e)
        } catch (e: RetrofitError) {
            throw ParticleCloudException(e)
        }

        return maxUsage
    }

    /**
     * Return the value for `variableName` on this Particle device.
     *
     *
     * Unless you specifically require generic handling, it is recommended that you use the
     * `get(type)Variable` methods instead, e.g.:  `getIntVariable()`.
     * These type-specific methods don't require extra casting or type checking on your part, and
     * they more clearly and succinctly express your intent.
     */
    @WorkerThread
    @Throws(ParticleCloudException::class, IOException::class, VariableDoesNotExistException::class)
    fun getVariable(variableName: String): Any? {

        val requester = object : VariableRequester<Any?, ReadObjectVariableResponse>(this) {
            override fun callApi(variableName: String): ReadObjectVariableResponse {
                return mainApi.getVariable(deviceState.deviceId, variableName)
            }
        }

        return requester.getVariable(variableName)
    }

    /**
     * Return the value for `variableName` as an int.
     *
     *
     * Where practical, this method is recommended over the generic [.getVariable].
     * See the javadoc on that method for details.
     */
    @WorkerThread
    @Throws(
        ParticleCloudException::class,
        IOException::class,
        VariableDoesNotExistException::class,
        ClassCastException::class
    )
    fun getIntVariable(variableName: String): Int {

        val requester = object : VariableRequester<Int, ReadIntVariableResponse>(this) {
            override fun callApi(variableName: String): ReadIntVariableResponse {
                return mainApi.getIntVariable(deviceState.deviceId, variableName)
            }
        }

        return requester.getVariable(variableName)
    }

    /**
     * Return the value for `variableName` as a String.
     *
     *
     * Where practical, this method is recommended over the generic [.getVariable].
     * See the javadoc on that method for details.
     */
    @WorkerThread
    @Throws(
        ParticleCloudException::class,
        IOException::class,
        VariableDoesNotExistException::class,
        ClassCastException::class
    )
    fun getStringVariable(variableName: String): String {

        val requester = object : VariableRequester<String, ReadStringVariableResponse>(this) {
            override fun callApi(variableName: String): ReadStringVariableResponse {
                return mainApi.getStringVariable(deviceState.deviceId, variableName)
            }
        }

        return requester.getVariable(variableName)
    }

    /**
     * Return the value for `variableName` as a double.
     *
     * Where practical, this method is recommended over the generic [.getVariable].
     * See the javadoc on that method for details.
     */
    @WorkerThread
    @Throws(
        ParticleCloudException::class,
        IOException::class,
        VariableDoesNotExistException::class,
        ClassCastException::class
    )
    fun getDoubleVariable(variableName: String): Double {
        val requester = object : VariableRequester<Double, ReadDoubleVariableResponse>(this) {
            override fun callApi(variableName: String): ReadDoubleVariableResponse {
                return mainApi.getDoubleVariable(deviceState.deviceId, variableName)
            }
        }
        return requester.getVariable(variableName)
    }


    /**
     * Call a function on the device
     *
     * @param functionName Function name
     * @param args         Array of arguments to pass to the function on the device.
     * Arguments must not be more than MAX_PARTICLE_FUNCTION_ARG_LENGTH chars
     * in length. If any arguments are longer, a runtime exception will be thrown.
     * @return result code: a value of 1 indicates success
     */
    @WorkerThread
    @Throws(ParticleCloudException::class, IOException::class, FunctionDoesNotExistException::class)
    @JvmOverloads
    fun callFunction(functionName: String, args: List<String>? = null): Int {
        var argz = args
        // TODO: check response of calling a non-existent function
        if (!deviceState.functions.contains(functionName)) {
            throw FunctionDoesNotExistException(functionName)
        }

        // null is accepted here, but it won't be in the Retrofit API call later
        if (args == null) {
            argz = list()
        }

        val argsString = join(argz, ',')
        Preconditions.checkArgument(
            (argsString?.length ?: 0) < MAX_PARTICLE_FUNCTION_ARG_LENGTH,
            String.format(
                "Arguments '%s' exceed max args length of %d",
                argsString, MAX_PARTICLE_FUNCTION_ARG_LENGTH
            )
        )

        val response: Responses.CallFunctionResponse
        try {
            response = mainApi.callFunction(
                deviceState.deviceId, functionName,
                FunctionArgs(argsString)
            )
        } catch (e: RetrofitError) {
            throw ParticleCloudException(e)
        }

        if (!response.connected) {
            cloud.onDeviceNotConnected(deviceState)
            throw IOException("Device is not connected.")
        } else {
            return response.returnValue
        }
    }

    /**
     * Subscribe to events from this device
     *
     * @param eventNamePrefix (optional, may be null) a filter to match against for events.  If
     * null or an empty string, all device events will be received by the handler
     * trigger eventHandler
     * @param handler         The handler for the events received for this subscription.
     * @return the subscription ID
     * (see [ParticleCloud.subscribeToAllEvents] for more info
     */
    @Throws(IOException::class)
    fun subscribeToEvents(eventNamePrefix: String?, handler: ParticleEventHandler): Long {
        log.d("Subscribing to events with prefix: $eventNamePrefix for device ${deviceState.deviceId}")
        return cloud.subscribeToDeviceEvents(eventNamePrefix, deviceState.deviceId, handler)
    }

    /**
     * Unsubscribe from events.
     *
     * @param eventListenerID The ID of the subscription to be cancelled. (returned from
     * [.subscribeToEvents]
     */
    @Throws(ParticleCloudException::class)
    fun unsubscribeFromEvents(eventListenerID: Long) {
        cloud.unsubscribeFromEventWithID(eventListenerID)
    }

    /**
     * Remove device from current logged in user account
     */
    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun unclaim() {
        try {
            cloud.unclaimDevice(deviceState.deviceId)
        } catch (e: RetrofitError) {
            throw ParticleCloudException(e)
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun flashKnownApp(knownApp: KnownApp) {
        performFlashingChange { mainApi.flashKnownApp(deviceState.deviceId, knownApp.appName) }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun flashBinaryFile(file: File) {
        performFlashingChange {
            mainApi.flashFile(
                deviceState.deviceId,
                TypedFile("application/octet-stream", file)
            )
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class, IOException::class)
    fun flashBinaryFile(stream: InputStream) {
        val bytes = stream.source().buffer().readByteArray()
        performFlashingChange { mainApi.flashFile(deviceState.deviceId, TypedFakeFile(bytes)) }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun flashCodeFile(file: File) {
        performFlashingChange {
            mainApi.flashFile(
                deviceState.deviceId,
                TypedFile("multipart/form-data", file)
            )
        }
    }


    @WorkerThread
    @Throws(ParticleCloudException::class, IOException::class)
    fun flashCodeFile(stream: InputStream) {
        val bytes = stream.source().buffer().readByteArray()
        performFlashingChange {
            mainApi.flashFile(
                deviceState.deviceId,
                TypedFakeFile(bytes, "multipart/form-data", "code.ino")
            )
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun refresh() {
        // just calling this get method will update everything as expected.
        log.i("refresh() for device ${deviceState.deviceId}")
        cloud.getDevice(deviceState.deviceId)
    }


    // FIXME: ugh.  these "cloud.notifyDeviceChanged();" calls are a hint that flashing maybe
    // should just live in a class of its own, or that it should just be a delegate on
    // ParticleCloud.  Review this later.
    @Throws(ParticleCloudException::class)
    private fun performFlashingChange(flashingChange: () -> Unit) {
        try {
            //listens for flashing event, on success unsubscribe from listening.
            subscribeToSystemEvent("spark/flash/status", object : SimpleParticleEventHandler {
                override fun onEvent(eventName: String, particleEvent: ParticleEvent) {
                    if ("success" == particleEvent.dataPayload) {
                        isFlashing = false
                        try {
                            this@ParticleDevice.refresh()
                            cloud.unsubscribeFromEventWithHandler(this)
                        } catch (e: ParticleCloudException) {
                            // not much else we can really do here...
                            log.w("Unable to reset flashing state for %s" + deviceState.deviceId, e)
                        }

                    } else {
                        isFlashing = true
                    }
                    cloud.notifyDeviceChanged()
                }
            })
            subscribeToSystemEvent("spark/device/app-hash", object : SimpleParticleEventHandler {
                override fun onEvent(eventName: String, particleEvent: ParticleEvent) {
                    isFlashing = false
                    try {
                        this@ParticleDevice.refresh()
                        cloud.unsubscribeFromEventWithHandler(this)
                    } catch (e: ParticleCloudException) {
                        // not much else we can really do here...
                        log.w("Unable to reset flashing state for %s" + deviceState.deviceId, e)
                    }
                    cloud.notifyDeviceChanged()
                }
            })
            flashingChange()
        } catch (e: RetrofitError) {
            throw ParticleCloudException(e)
        } catch (e: IOException) {
            throw ParticleCloudException(e)
        }

    }

    /**
     * Subscribes to system events of current device. Events emitted to EventBus listener.
     *
     * @throws ParticleCloudException Failure to subscribe to system events.
     * @see [EventBus](https://github.com/greenrobot/EventBus)
     */
    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun subscribeToSystemEvents() {
        try {
            val eventBus = EventBus.getDefault()
            subscriptions.add(
                subscribeToSystemEvent("spark/status") { _, particleEvent ->
                    particleEvent.dataPayload?.let {
                        sendUpdateStatusChange(eventBus, particleEvent.dataPayload)
                    }
                }
            )
            subscriptions.add(
                subscribeToSystemEvent("spark/flash/status") { _, particleEvent ->
                    particleEvent.dataPayload?.let {
                        sendUpdateFlashChange(eventBus, particleEvent.dataPayload)
                    }
                }
            )
            subscriptions.add(
                subscribeToSystemEvent("spark/device/app-hash") { _, _ ->
                    sendSystemEventBroadcast(
                        DeviceStateChange(
                            this@ParticleDevice,
                            ParticleDeviceState.APP_HASH_UPDATED
                        ), eventBus
                    )
                }
            )
            subscriptions.add(
                subscribeToSystemEvent("spark/status/safe-mode") { _, _ ->
                    sendSystemEventBroadcast(
                        DeviceStateChange(
                            this@ParticleDevice,
                            ParticleDeviceState.SAFE_MODE_UPDATER
                        ), eventBus
                    )
                }
            )
            subscriptions.add(
                subscribeToSystemEvent("spark/safe-mode-updater/updating") { _, _ ->
                    sendSystemEventBroadcast(
                        DeviceStateChange(
                            this@ParticleDevice,
                            ParticleDeviceState.ENTERED_SAFE_MODE
                        ), eventBus
                    )
                }
            )
        } catch (e: IOException) {
            val ex = ParticleCloudException(e)
            log.d("Failed to auto-subscribe to system events", ex)
            throw ex
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun startStopSignaling(shouldSignal: Boolean) {
        val signalInt = if (shouldSignal) 1 else 0
        try {
            mainApi.shoutRainbows(deviceState.deviceId, signalInt)
        } catch (e: RetrofitError) {
            throw ParticleCloudException(e)
        }
    }

    /**
     * Ping the device, updating its online/offline state
     *
     * @return true if online, else false
     */
    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun pingDevice(): Boolean {
        try {
            val response = mainApi.pingDevice(deviceState.deviceId, "requisite_put_body")

            // FIXME: update device state here after switching to Kotlin

            return response.online
        } catch (e: RetrofitError) {
            throw ParticleCloudException(e)
        }
    }


    private fun sendSystemEventBroadcast(deviceStateChange: DeviceStateChange, eventBus: EventBus) {
        cloud.sendSystemEventBroadcast(deviceStateChange)
        eventBus.post(deviceStateChange)
    }

    /**
     * Unsubscribes from system events of current device.
     *
     * @throws ParticleCloudException Failure to unsubscribe from system events.
     */
    @Throws(ParticleCloudException::class)
    fun unsubscribeFromSystemEvents() {
        for (subscriptionId in subscriptions) {
            unsubscribeFromEvents(subscriptionId!!)
        }
    }



    @Throws(IOException::class)
    private fun subscribeToSystemEvent(
        eventNamePrefix: String,
        particleEventHandler: SimpleParticleEventHandler
    ): Long {
        //Error would be handled in same way for every event name prefix, thus only simple onEvent listener is needed
        return subscribeToEvents(eventNamePrefix, object : ParticleEventHandler {
            override fun onEvent(eventName: String, particleEvent: ParticleEvent) {
                particleEventHandler.onEvent(eventName, particleEvent)
            }

            override fun onEventError(e: Exception) {
                log.d("Event error in system event handler")
            }
        })
    }

    @Throws(IOException::class)
    private fun subscribeToSystemEvent(
        eventNamePrefix: String,
        particleEventHandler: (String, ParticleEvent) -> Unit
    ): Long {
        log.d("subscribeToSystemEvent: $eventNamePrefix")
        //Error would be handled in same way for every event name prefix, thus only simple onEvent listener is needed
        return subscribeToEvents(eventNamePrefix, object : ParticleEventHandler {
            override fun onEvent(eventName: String, particleEvent: ParticleEvent) {
                particleEventHandler(eventName, particleEvent)
            }

            override fun onEventError(e: Exception) {
                log.d("Event error in system event handler")
            }
        })
    }

    private fun sendUpdateStatusChange(eventBus: EventBus, data: String) {
        val deviceStateChange = when (data) {
            "online" -> DeviceStateChange(this, ParticleDeviceState.CAME_ONLINE)
            "offline" -> DeviceStateChange(this, ParticleDeviceState.WENT_OFFLINE)
            else -> { throw IllegalArgumentException("Unrecognized status string: $data") }
        }
        sendSystemEventBroadcast(deviceStateChange, eventBus)
    }

    private fun sendUpdateFlashChange(eventBus: EventBus, data: String) {
        val deviceStateChange = when (data) {
            "started" -> DeviceStateChange(this, ParticleDeviceState.FLASH_STARTED)
            "success" -> DeviceStateChange(this, ParticleDeviceState.FLASH_SUCCEEDED)
            else -> { throw IllegalArgumentException("Unrecognized flash change string: $data") }
        }
        sendSystemEventBroadcast(deviceStateChange, eventBus)
    }

    override fun toString(): String {
        return "ParticleDevice{" +
                "deviceState=" + deviceState + "}"
    }

    //region Parcelable
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(deviceState, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ParticleDevice

        if (deviceState != other.deviceState) return false

        return true
    }

    override fun hashCode(): Int {
        return deviceState.hashCode()
    }
    //endregion


    private class TypedFakeFile @JvmOverloads constructor(
        bytes: ByteArray,
        mimeType: String = "application/octet-stream",
        private val fileName: String = "tinker_firmware.bin"
    ) : TypedByteArray(mimeType, bytes) {

        override fun fileName(): String {
            return fileName
        }
    }

    /**
     * Constructs a new typed byte array.  Sets mimeType to `application/unknown` if absent.
     *
     * @throws NullPointerException if bytes are null
     */


    private abstract class VariableRequester<T, R : ReadVariableResponse<T>> internal constructor(
        private val device: ParticleDevice
    ) {

        @WorkerThread
        internal abstract fun callApi(variableName: String): R


        @WorkerThread
        @Throws(
            ParticleCloudException::class,
            IOException::class,
            VariableDoesNotExistException::class
        )
        internal fun getVariable(variableName: String): T {

            if (!device.deviceState.variables.containsKey(variableName)) {
                throw VariableDoesNotExistException(variableName)
            }

            val reply: R
            try {
                reply = callApi(variableName)
            } catch (e: RetrofitError) {
                throw ParticleCloudException(e)
            }

            if (!reply.coreInfo.connected) {
                // FIXME: we should be doing this "connected" check on _any_ reply that comes back
                // with a "coreInfo" block.
                device.cloud.onDeviceNotConnected(device.deviceState)
                throw IOException("Device is not connected.")
            } else {
                return reply.result
            }
        }

    }

    companion object {

        private const val MAX_PARTICLE_FUNCTION_ARG_LENGTH = 622

        private val log = TLog.get(ParticleDevice::class.java)

        @JvmField
        val CREATOR: Parcelable.Creator<ParticleDevice> = object : Parcelable.Creator<ParticleDevice> {
                override fun createFromParcel(`in`: Parcel): ParticleDevice {
                    val sdkProvider = ParticleCloudSDK.getSdkProvider()
                    val deviceState = `in`.readParcelable<DeviceState>(DeviceState::class.java.classLoader)
                    return sdkProvider.particleCloud.getDeviceFromState(deviceState!!)
                }

                override fun newArray(size: Int): Array<ParticleDevice?> {
                    return arrayOfNulls(size)
                }
            }
    }

}
