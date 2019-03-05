package io.particle.mesh.ota

import androidx.annotation.WorkerThread
import com.squareup.okhttp.OkHttpClient
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.mesh.common.Result
import io.particle.mesh.setup.connection.ProtocolTransceiver
import io.particle.mesh.setup.connection.ResultCode
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
        deviceType: ParticleDeviceType
    ): Boolean {
        return getUpdateUrl(xceiver, deviceType) != null
    }

    @WorkerThread
    suspend fun startUpdateIfNecessary(
        xceiver: ProtocolTransceiver,
        deviceType: ParticleDeviceType,
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
        delay(10000)

        return FirmwareUpdateResult.DEVICE_IS_UPDATING
    }

    @WorkerThread
    private suspend fun getUpdateUrl(
        xceiver: ProtocolTransceiver,
        deviceType: ParticleDeviceType
    ): URL? {
        val systemFwVers = xceiver.sendGetSystemFirmwareVersion().throwOnErrorOrAbsent()
        log.info { "Getting update URL for device currently on firmware version ${systemFwVers.version}" }
        val (ncpVersion, ncpModuleVersion) = getNcpVersions(xceiver)

        val updateUrl = cloud.getFirmwareUpdateInfo(
            deviceType.intValue,
            systemFwVers.version,
            ncpVersion,
            ncpModuleVersion
        )

        log.info { "Update URL for device $updateUrl" }

        return updateUrl
    }

    private suspend fun getNcpVersions(xceiver: ProtocolTransceiver): Pair<String?, Int?> {
        val ncpFwReply = xceiver.sendGetNcpFirmwareVersion()
        return when (ncpFwReply) {
            is Result.Present -> Pair(ncpFwReply.value.version, ncpFwReply.value.moduleVersion)
            is Result.Error -> {
                if (ncpFwReply.error == ResultCode.NOT_SUPPORTED) {
                    Pair(null, null)
                } else {
                    throw IllegalStateException("Error getting NCP FW version: ${ncpFwReply.error}")
                }
            }
            is Result.Absent -> throw IllegalStateException("No result received!")
        }
    }
}