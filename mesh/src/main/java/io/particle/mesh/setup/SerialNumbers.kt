package io.particle.mesh.setup

import androidx.annotation.WorkerThread
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType


inline class SerialNumber(val value: String)


fun SerialNumber.isSomSerial(): Boolean {
    return value.toLowerCase().startsWith("p00")
}


@WorkerThread
fun SerialNumber.toDeviceType(cloud: ParticleCloud): ParticleDeviceType {

    fun SerialNumber.toDeviceType(): ParticleDeviceType {
        val first4 = this.value.substring(0, 4)
        return when (first4) {
            ARGON_SERIAL_PREFIX1,
            ARGON_SERIAL_PREFIX2,
            ARGON_SERIAL_PREFIX3 -> ParticleDeviceType.ARGON
            BORON_LTE_SERIAL_PREFIX1,
            BORON_LTE_SERIAL_PREFIX2,
            BORON_3G_SERIAL_PREFIX1,
            BORON_3G_SERIAL_PREFIX2 -> ParticleDeviceType.BORON
            XENON_SERIAL_PREFIX1,
            XENON_SERIAL_PREFIX2 -> ParticleDeviceType.XENON
            A_SERIES_SERIAL_PREFIX -> ParticleDeviceType.A_SERIES
            B_SERIES_LTE_SERIAL_PREFIX1,
            B_SERIES_3G_SERIAL_PREFIX2 -> ParticleDeviceType.B_SERIES
            X_SERIES_SERIAL_PREFIX -> ParticleDeviceType.X_SERIES
            else -> throw IllegalArgumentException("Invalid serial number from barcode: $this")
        }
    }

    return try {
        return this.toDeviceType()
    } catch (badArg: IllegalArgumentException) {
        cloud.getPlatformId(this.value)
    }
}
