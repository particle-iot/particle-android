package io.particle.commonui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ARGON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.A_SOM
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


val ParticleDeviceType.productImage: Int
    get() = this.productImageAndName.image


val ParticleDeviceType.productName: Int
    get() = this.productImageAndName.name


private data class ProductImageAndName(@StringRes val name: Int, @DrawableRes val image: Int)

private val ParticleDeviceType.productImageAndName: ProductImageAndName
    get() {
        return when (this) {
            CORE -> pian(R.string.product_name_core, R.drawable.product_image_core)

            PHOTON -> pian(R.string.product_name_photon, R.drawable.product_image_photon)

            ELECTRON -> pian(R.string.product_name_electron, R.drawable.product_image_electron)

            RASPBERRY_PI -> pian(R.string.product_name_raspberry, R.drawable.product_image_pi)

            P1 -> pian(R.string.product_name_p1, R.drawable.product_image_p1)

            RED_BEAR_DUO -> pian(R.string.product_name_red_bear_duo, R.drawable.product_image_red_bear_duo)

            ARGON -> pian(R.string.product_name_argon, R.drawable.product_image_argon)

            A_SOM -> pian(R.string.product_name_a_series, R.drawable.product_image_argon)

            BORON -> pian(R.string.product_name_boron, R.drawable.product_image_boron)

            B_SOM -> pian(R.string.product_name_b_series, R.drawable.product_image_boron)

            XENON -> pian(R.string.product_name_xenon, R.drawable.product_image_xenon)

            X_SOM -> pian(R.string.product_name_x_series, R.drawable.product_image_xenon)

            // ALL OTHERS
            ESP32,
            BLUZ,
            DIGISTUMP_OAK,
            OTHER -> pian(R.string.product_name_unknown, R.drawable.product_image_unknown)
        }
    }


private fun pian(@StringRes name: Int, @DrawableRes image: Int): ProductImageAndName {
    return ProductImageAndName(name, image)
}