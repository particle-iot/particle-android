package io.particle.mesh.setup

import androidx.annotation.WorkerThread
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import mu.KotlinLogging


private val log = KotlinLogging.logger {}


inline class SerialNumber(val value: String)



@WorkerThread
fun SerialNumber.toDeviceType(cloud: ParticleCloud): ParticleDeviceType {
    return cloud.getPlatformId(this.value)
}
