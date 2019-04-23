package io.particle.android.sdk.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ARGON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.A_SERIES
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.BLUZ
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.BORON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.B_SERIES
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.CORE
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.DIGISTUMP_OAK
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ELECTRON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.OTHER
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.P1
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.PHOTON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.RASPBERRY_PI
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.RED_BEAR_DUO
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.XENON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.X_SERIES
import io.particle.sdk.app.R


val ParticleDeviceType.productImage: Int
    get() = this.productImageAndName.image


val ParticleDeviceType.productName: Int
    get() = this.productImageAndName.name


private data class ProductImageAndName(@StringRes val name: Int, @DrawableRes val image: Int)

private val ParticleDeviceType.productImageAndName: ProductImageAndName
    get() {
        return when (this) {
            CORE -> pin(R.string.core, R.drawable.core_vector)

            PHOTON -> pin(R.string.photon, R.drawable.photon_vector_small)

            ELECTRON -> pin(R.string.electron, R.drawable.electron_vector_small)

            RASPBERRY_PI -> pin(R.string.raspberry, R.drawable.pi_vector)

            P1 -> pin(R.string.p1, R.drawable.p1_vector)

            RED_BEAR_DUO -> pin(R.string.red_bear_duo, R.drawable.red_bear_duo_vector)

            ARGON,
            A_SERIES -> pin(R.string.product_name_argon, R.drawable.argon_vector)

            BORON,
            B_SERIES -> pin(R.string.product_name_boron, R.drawable.boron_vector)

            XENON,
            X_SERIES -> pin(R.string.product_name_xenon, R.drawable.xenon_vector)

            // ALL OTHERS
            BLUZ,
            DIGISTUMP_OAK,
            OTHER -> pin(R.string.unknown, R.drawable.unknown_vector)
        }
    }


private fun pin(@StringRes name: Int, @DrawableRes image: Int): ProductImageAndName {
    return ProductImageAndName(name, image)
}