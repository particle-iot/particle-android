package io.particle.mesh.setup

import androidx.annotation.WorkerThread
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.mesh.setup.flow.Gen3ConnectivityType


@WorkerThread
fun BarcodeData.toDeviceType(cloud: ParticleCloud): ParticleDeviceType {
    return this.serialNumber.toDeviceType(cloud)
}


@WorkerThread
fun BarcodeData.toConnectivityType(cloud: ParticleCloud): Gen3ConnectivityType {
    return this.serialNumber.toDeviceType(cloud).toConnectivityType()
}


fun ParticleDeviceType.toConnectivityType(): Gen3ConnectivityType {
    return when (this) {
        ParticleDeviceType.ARGON,
        ParticleDeviceType.A_SERIES -> Gen3ConnectivityType.WIFI
        ParticleDeviceType.BORON,
        ParticleDeviceType.B_SERIES -> Gen3ConnectivityType.CELLULAR
        ParticleDeviceType.XENON,
        ParticleDeviceType.X_SERIES -> Gen3ConnectivityType.MESH_ONLY
        else -> throw IllegalArgumentException("Not a mesh device: $this")
    }
}


sealed class BarcodeData {

    abstract val serialNumber: SerialNumber


    data class CompleteBarcodeData (
        override val serialNumber: SerialNumber,
        val mobileSecret: String
    ) : BarcodeData()


    data class PartialBarcodeData (
        override val serialNumber: SerialNumber,
        val partialMobileSecret: String
    ) : BarcodeData()


    companion object {

        fun fromRawData(rawBarcodeData: String?): BarcodeData? {
            if (rawBarcodeData == null) {
                return null
            }

            val split: List<String> = rawBarcodeData.split(" ")

            // we should have one and only one space char
            if (split.size != 2) {
                return null
            }

            val serialValue = split[0]
            val mobileSecret = split[1]

            if (serialValue.length != 15) {
                return null  // serial number must be exactly 15 chars
            }

            val serial = SerialNumber(serialValue)
            return if (mobileSecret.length == 15) {
                CompleteBarcodeData(serial, mobileSecret)
            } else {
                PartialBarcodeData(serial, mobileSecret)
            }
        }
    }
}