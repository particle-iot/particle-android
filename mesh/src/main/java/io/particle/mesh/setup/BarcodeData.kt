package io.particle.mesh.setup

import androidx.annotation.WorkerThread
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ARGON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.A_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.B5_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.BLUZ
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.BORON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.B_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.CORE
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.DIGISTUMP_OAK
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ELECTRON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ESP32
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.OTHER
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.P1
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.PHOTON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.RASPBERRY_PI
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.RED_BEAR_DUO
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.XENON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.X_SOM
import io.particle.mesh.setup.BarcodeData.CompleteBarcodeData
import io.particle.mesh.setup.flow.Gen3ConnectivityType
import io.particle.mesh.setup.flow.Gen3ConnectivityType.CELLULAR
import io.particle.mesh.setup.flow.Gen3ConnectivityType.MESH_ONLY
import io.particle.mesh.setup.flow.Gen3ConnectivityType.WIFI


@WorkerThread
fun BarcodeData.toDeviceType(cloud: ParticleCloud): ParticleDeviceType {
    return this.serialNumber.toDeviceType(cloud)
}


fun ParticleDeviceType.toConnectivityType(): Gen3ConnectivityType {
    return when (this) {
        OTHER,
        CORE,
        PHOTON,
        P1,
        RASPBERRY_PI,
        RED_BEAR_DUO,
        BLUZ,
        DIGISTUMP_OAK,
        ESP32,
        ARGON,
        A_SOM -> WIFI

        ELECTRON,
        BORON,
        B_SOM,
        B5_SOM -> CELLULAR

        XENON,
        X_SOM -> MESH_ONLY
    }
}


sealed class BarcodeData {

    abstract val serialNumber: SerialNumber


    data class CompleteBarcodeData(
        override val serialNumber: SerialNumber,
        val mobileSecret: String
    ) : BarcodeData()


    data class PartialBarcodeData(
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


@WorkerThread
fun ParticleCloud.fetchBarcodeData(deviceId: String): CompleteBarcodeData {
    val device = this.getDevice(deviceId)
    return CompleteBarcodeData(
        serialNumber = SerialNumber(device.serialNumber!!),
        mobileSecret = device.mobileSecret!!
    )
}