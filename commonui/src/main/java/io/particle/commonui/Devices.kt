package io.particle.commonui

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


val ParticleDeviceType.productImage: Int
    get() = this.productImageAndName.image


val ParticleDeviceType.productName: Int
    get() = this.productImageAndName.name


private data class ProductImageAndName(@StringRes val name: Int, @DrawableRes val image: Int)

private val ParticleDeviceType.productImageAndName: ProductImageAndName
    get() {
        return when (this) {
            CORE -> pin(R.string.product_name_core, R.drawable.product_image_core)

            PHOTON -> pin(R.string.product_name_photon, R.drawable.product_image_photon)

            ELECTRON -> pin(R.string.product_name_electron, R.drawable.product_image_electron)

            RASPBERRY_PI -> pin(R.string.product_name_raspberry, R.drawable.product_image_pi)

            P1 -> pin(R.string.product_name_p1, R.drawable.product_image_p1)

            RED_BEAR_DUO -> pin(R.string.product_name_red_bear_duo, R.drawable.product_image_red_bear_duo)

            ARGON,
            A_SERIES -> pin(R.string.product_name_argon, R.drawable.product_image_argon)

            BORON,
            B_SERIES -> pin(R.string.product_name_boron, R.drawable.product_image_boron)

            XENON,
            X_SERIES -> pin(R.string.product_name_xenon, R.drawable.product_image_xenon)

            // ALL OTHERS
            BLUZ,
            DIGISTUMP_OAK,
            OTHER -> pin(R.string.product_name_unknown, R.drawable.product_image_unknown)
        }
    }


private fun pin(@StringRes name: Int, @DrawableRes image: Int): ProductImageAndName {
    return ProductImageAndName(name, image)
}