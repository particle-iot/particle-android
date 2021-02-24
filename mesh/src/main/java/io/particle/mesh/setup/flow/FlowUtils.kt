package io.particle.mesh.setup.flow

import androidx.annotation.StringRes
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ARGON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.A_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.B5_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.BORON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.B_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.XENON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.X_SOM
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.mesh.R
import kotlinx.coroutines.delay
import mu.KotlinLogging
import retrofit.RetrofitError
import java.net.SocketTimeoutException

// Junk-drawer classes aren't great, but these functions doesn't really belong anywhere else.

@StringRes
fun ParticleDeviceType.toUserFacingName(): Int {
    return when (this) {
        ARGON -> R.string.product_name_argon
        BORON -> R.string.product_name_boron
        XENON -> R.string.product_name_xenon
        A_SOM -> R.string.product_name_a_series
        B_SOM -> R.string.product_name_b_series
        B5_SOM -> R.string.product_name_b5_som
        X_SOM -> R.string.product_name_x_series
        else -> throw IllegalArgumentException("Not a mesh device: $this")
    }
}


private val log = KotlinLogging.logger {}


internal suspend fun retrySimAction(simActionBlock: () -> Unit) {

    var error: Exception? = null

    for (i in 1..SIM_ACTION_MAX_RETRY_COUNT) {

        log.info { "Attempting SIM action, retry count=$i" }

        try {
            simActionBlock()
            error = null
            return

        } catch (ex: ParticleCloudException) {
            error = ex
            if (ex.responseData?.httpStatusCode == 504 || ex.responseData?.httpStatusCode == 408) {
                // this is apparently considered OK from the API and we should just retry...
                delay(1000)
                continue
            }

            // FIXME: verify that this is OK, too
            if (ex.cause is RetrofitError
                && (ex.cause as RetrofitError).cause is SocketTimeoutException
            ) {
                delay(1000)
                continue
            }

            // if it's none of those errors, just bail
            throw ex
        }
    }

    if (error != null) {
        throw error
    }
}