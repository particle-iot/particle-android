package io.particle.commonui

import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
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


fun TextView.styleAsPill(deviceType: ParticleDeviceType) {
    @ColorInt val colorValue: Int = ContextCompat.getColor(
        context,
        deviceType.toDecorationColor()
    )
    val bg = this.background
    bg.mutate()
    DrawableCompat.setTint(bg, colorValue)
    this.setTextColor(colorValue)
    this.setText(deviceType.productName)
}


// this would be an extension property, but you can't use annotations like @ColorRes on
// extension properties
@ColorRes
fun ParticleDeviceType.toDecorationColor(): Int {
    return when (this) {
        ELECTRON -> R.color.device_color_electron

        PHOTON, P1 -> R.color.device_color_photon

        ARGON, A_SOM -> R.color.spark_blue

        BORON, B_SOM -> R.color.device_color_boron

        XENON, X_SOM -> R.color.device_color_xenon

        CORE,
        RASPBERRY_PI,
        RED_BEAR_DUO,
        ESP32,
        BLUZ,
        DIGISTUMP_OAK,
        OTHER -> R.color.device_color_other
    }
}


fun ParticleDeviceType.toDecorationLetter(): String {
    return when (this) {
        CORE -> "C"
        ELECTRON -> "E"
        PHOTON -> "P"
        P1 -> "1"
        RASPBERRY_PI -> "R"
        RED_BEAR_DUO -> "D"
        ESP32 -> "ES"
        BLUZ -> "BZ"
        ARGON,
        A_SOM -> "A"
        BORON,
        B_SOM -> "B"
        XENON,
        X_SOM -> "X"
        DIGISTUMP_OAK,
        OTHER -> "?"
    }
}
