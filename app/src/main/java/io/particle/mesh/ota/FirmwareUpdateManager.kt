package io.particle.mesh.ota

import androidx.annotation.WorkerThread
import com.squareup.okhttp.OkHttpClient
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.firmwareprotos.ctrl.Common.ResultCode
import io.particle.mesh.common.Result
import io.particle.mesh.setup.connection.ProtocolTransceiver
import io.particle.mesh.setup.flow.MeshDeviceType
import io.particle.mesh.setup.flow.throwOnErrorOrAbsent
import kotlinx.coroutines.delay
import mu.KotlinLogging
import java.net.URL


enum class FirmwareUpdateResult {
    HAS_LATEST_FIRMWARE,
    DEVICE_IS_UPDATING
}


class FirmwareUpdateManager(
    private val cloud: ParticleCloud,
    private val okHttpClient: OkHttpClient
) {

    private val log = KotlinLogging.logger {}

    @WorkerThread
    suspend fun needsUpdate(
        xceiver: ProtocolTransceiver,
        deviceType: MeshDeviceType
    ): Boolean {
        return getUpdateUrl(xceiver, deviceType) != null
    }

    @WorkerThread
    suspend fun startUpdateIfNecessary(
        xceiver: ProtocolTransceiver,
        deviceType: MeshDeviceType,
        listener: ProgressListener
    ): FirmwareUpdateResult {

        val firmwareUpdateUrl = getUpdateUrl(xceiver, deviceType)
        if (firmwareUpdateUrl == null) {
            return FirmwareUpdateResult.HAS_LATEST_FIRMWARE
        }

        val updater = FirmwareUpdater(xceiver, okHttpClient)
        updater.updateFirmware(firmwareUpdateUrl) {
            log.debug { "Firmware progress: $it" }
            listener(it)
        }

        // pause a bit before disconnecting...
        delay(2000)
        xceiver.disconnect()
        // and after disconnecting...
        delay(8000)

        return FirmwareUpdateResult.DEVICE_IS_UPDATING
    }

    @WorkerThread
    private suspend fun getUpdateUrl(
        xceiver: ProtocolTransceiver,
        deviceType: MeshDeviceType
    ): URL? {
        val systemFwVers = xceiver.sendGetSystemFirmwareVersion().throwOnErrorOrAbsent()
        val (ncpVersion, ncpModuleVersion) = getNcpVersions(xceiver)

        return cloud.getFirmwareUpdateInfo(
            deviceType.particleDeviceType.platformId,
            systemFwVers.version,
            ncpVersion,
            ncpModuleVersion
        )
    }

    private suspend fun getNcpVersions(xceiver: ProtocolTransceiver): Pair<String?, Int?> {
        val ncpFwReply = xceiver.sendGetNcpFirmwareVersion()
        return when (ncpFwReply) {
            is Result.Present -> Pair(ncpFwReply.value.version, ncpFwReply.value.moduleVersion)
            is Result.Error -> {
                // FIXME: this error should be "NOT_SUPPORTED"
                if (ncpFwReply.error == ResultCode.UNRECOGNIZED) {
                    Pair(null, null)
                } else {
                    throw IllegalStateException("Error getting NCP FW version: ${ncpFwReply.error}")
                }
            }
            is Result.Absent -> throw IllegalStateException("No result received!")
        }
    }
}


private val ParticleDeviceType.platformId: Int
    get() = when (this) {
        ParticleDeviceType.ARGON -> 12
        ParticleDeviceType.BORON -> 13
        ParticleDeviceType.XENON -> 14
        else -> throw IllegalArgumentException("${this.name} is not a mesh device!")
    }
