package io.particle.android.sdk.cloud

import android.os.Parcelable
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.cloud.ParticleDevice.VariableType
import kotlinx.android.parcel.Parcelize
import java.util.*


@Parcelize
internal data class DeviceState(
    internal val deviceId: String,
    internal val platformId: Int?,
    internal val productId: Int?,
    internal val ipAddress: String?,
    internal val lastAppName: String?,
    internal val status: String?,
    internal val name: String?,
    internal val isConnected: Boolean?,
    internal val cellular: Boolean?,
    internal val imei: String?,
    internal val lastIccid: String?,
    internal val currentBuild: String?,
    internal val defaultBuild: String?,
    internal val functions: Set<String>,
    internal val variables: Map<String, VariableType>,
    internal val deviceType: ParticleDeviceType?,
    internal val lastHeard: Date?,
    internal val serialNumber: String?,
    internal val mobileSecret: String?,
    internal val iccid: String?,
    internal val systemFirmwareVersion: String?,
    internal val notes: String?
) : Parcelable {

    override fun toString(): String {
        return "DeviceState(deviceId='$deviceId', " +
                "platformId=$platformId, " +
                "productId=$productId, " +
                "ipAddress=$ipAddress, " +
                "lastAppName=$lastAppName, " +
                "status=$status, " +
                "name=$name, " +
                "isConnected=$isConnected, " +
                "cellular=$cellular, " +
                "imei=$imei, " +
                "lastIccid=$lastIccid, " +
                "currentBuild=$currentBuild, " +
                "defaultBuild=$defaultBuild, " +
                "functions=$functions, " +
                "variables=$variables, " +
                "deviceType=$deviceType, " +
                "lastHeard=$lastHeard, " +
                "serialNumber=$serialNumber, " +
                "mobileSecret=$mobileSecret, " +
                "iccid=$iccid, " +
                "systemFirmwareVersion=$systemFirmwareVersion, " +
                "notes=$notes)"
    }
}