package io.particle.mesh.setup.flow

import androidx.annotation.StringRes
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ARGON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.A_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.BORON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.B_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.XENON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.X_SOM
import io.particle.mesh.R

// Junk-drawer classes aren't great, but this function doesn't really belong anywhere else.

@StringRes
fun ParticleDeviceType.toUserFacingName(): Int {
    return when (this) {
        ARGON -> R.string.product_name_argon
        BORON -> R.string.product_name_boron
        XENON -> R.string.product_name_xenon
        A_SOM -> R.string.product_name_a_series
        B_SOM -> R.string.product_name_b_series
        X_SOM -> R.string.product_name_x_series
        else -> throw IllegalArgumentException("Not a mesh device: $this")
    }
}
