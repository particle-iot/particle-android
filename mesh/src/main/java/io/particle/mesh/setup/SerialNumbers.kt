package io.particle.mesh.setup

import androidx.annotation.WorkerThread
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import mu.KotlinLogging
import java.util.*


private val log = KotlinLogging.logger {}


inline class SerialNumber(val value: String)



@WorkerThread
fun SerialNumber.toDeviceType(cloud: ParticleCloud): ParticleDeviceType {

    fun SerialNumber.toDeviceType(): ParticleDeviceType {
        return when (this.value.take(4)) {
            in ARGON_SERIAL_PREFIXES -> ParticleDeviceType.ARGON
            in BORON_SERIAL_PREFIXES -> ParticleDeviceType.BORON
            in XENON_SERIAL_PREFIXES -> ParticleDeviceType.XENON
            in A_SERIES_SERIAL_PREFIXES -> ParticleDeviceType.A_SOM
            in B_SERIES_SERIAL_PREFIXES -> ParticleDeviceType.B_SOM
            in X_SERIES_SERIAL_PREFIXES -> ParticleDeviceType.X_SOM
            else -> throw IllegalArgumentException("Invalid serial number from barcode: $this")
        }
    }

    var gotTypeFromCloud = false
    val dt = try {
        this.toDeviceType()
    } catch (badArg: IllegalArgumentException) {
        gotTypeFromCloud = true
        cloud.getPlatformId(this.value)
    }

    val source = if (gotTypeFromCloud) "cloud" else "local serial number lookup"
    log.info { "Retrieved device type $dt from $source" }

    return dt
}
