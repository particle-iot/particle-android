package io.particle.android.sdk.tinker

import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.tinker.DeviceUiState.FLASHING
import io.particle.android.sdk.tinker.DeviceUiState.OFFLINE
import io.particle.android.sdk.tinker.DeviceUiState.ONLINE_NOT_USING_TINKER
import io.particle.android.sdk.tinker.DeviceUiState.ONLINE_USING_TINKER
import io.particle.sdk.app.R.id


enum class DeviceUiState(val contentViewId: Int) {
    ONLINE_USING_TINKER(id.tinker_content_view),
    ONLINE_NOT_USING_TINKER(id.flash_tinker_frame),
    OFFLINE(id.device_offline_text),
    FLASHING(id.flashing_progress_spinner);
}

val ParticleDevice.uiState: DeviceUiState
    get() {
        return when {
            !this.isConnected -> OFFLINE
            this.isFlashing -> FLASHING
            this.isRunningTinker -> ONLINE_USING_TINKER
            else -> ONLINE_NOT_USING_TINKER
        }
    }
