package io.particle.android.sdk.cloud

import android.os.Parcel
import android.os.Parcelable
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.cloud.ParticleDevice.VariableType
import io.particle.android.sdk.utils.*
import java.util.*


internal class DeviceState(
    val deviceId: String?,
    val name: String?,
    val isConnected: Boolean?,
    val cellular: Boolean?,
    val imei: String?,
    val lastIccid: String?,
    val currentBuild: String?,
    val defaultBuild: String?,
    val platformId: Int?,
    val productId: Int?,
    val ipAddress: String?,
    val status: String?,
    val lastHeard: Date?,
    val variables: Map<String, VariableType>?,
    val functions: Set<String>?,
    val version: String?,
    val lastAppName: String?,
    val requiresUpdate: Boolean?,
    val deviceType: ParticleDevice.ParticleDeviceType?
) : Parcelable {

    override fun writeToParcel(dest: Parcel, flags: Int) {
        val vars: Map<String, VariableType>? = variables
        val hasVariables = (vars != null)

        val funcs: Set<String>? = functions
        val hasFunctions = (funcs != null)

        dest.writeString(deviceId)
        dest.writeValue(name)
        dest.writeValue(isConnected)
        dest.writeValue(cellular)
        dest.writeValue(imei)
        dest.writeValue(lastIccid)
        dest.writeValue(currentBuild)
        dest.writeValue(defaultBuild)
        dest.writeValue(platformId)
        dest.writeValue(productId)
        dest.writeValue(ipAddress)
        dest.writeValue(status)

        dest.writeValue(lastHeard)

        dest.writeBoolean(hasVariables)
        if (hasVariables) {
            dest.writeSerializableMap(vars!!)
        }

        dest.writeBoolean(hasFunctions)
        if (hasFunctions) {
            dest.writeStringList(ArrayList(funcs!!))
        }

        dest.writeValue(version)
        dest.writeValue(lastAppName)
        dest.writeValue(requiresUpdate)
        dest.writeValue(deviceType?.name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DeviceState> {

        override fun createFromParcel(src: Parcel): DeviceState {
            return fromParcel(src)
        }

        override fun newArray(size: Int): Array<DeviceState?> {
            return arrayOfNulls(size)
        }

        private fun fromParcel(src: Parcel): DeviceState {

            fun readVars(): Map<String, ParticleDevice.VariableType>? {
                return if (src.readBoolean()) src.readSerializableMap() else null
            }

            fun readFuncs(): Set<String>? {
                return if (src.readBoolean()) src.readStringList().toSet() else null
            }

            fun readDeviceType(): ParticleDeviceType? {
                val stringValue: String? = src.readNullableValue()
                return if (stringValue == null) null else ParticleDeviceType.valueOf(stringValue)
            }

            return DeviceState(
                deviceId = src.readString(),
                name = src.readNullableValue(),
                isConnected = src.readNullableValue(),
                cellular = src.readNullableValue(),
                imei = src.readNullableValue(),
                lastIccid = src.readNullableValue(),
                currentBuild = src.readNullableValue(),
                defaultBuild = src.readNullableValue(),
                platformId = src.readNullableValue(),
                productId = src.readNullableValue(),
                ipAddress = src.readNullableValue(),
                status = src.readNullableValue(),
                lastHeard = src.readNullableValue(),
                variables = readVars(),
                functions = readFuncs(),
                version = src.readNullableValue(),
                lastAppName = src.readNullableValue(),
                requiresUpdate = src.readNullableValue(),
                deviceType = readDeviceType()
            )
        }
    }
}


private inline fun <reified T> Parcel.readNullableValue(): T? {
    return this.readValue(T::class.java.classLoader) as T
}
