package io.particle.mesh.setup.flow.modules.device

import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.mesh.ota.FirmwareUpdateManager
import io.particle.mesh.ota.FirmwareUpdateResult
import io.particle.mesh.setup.connection.ProtocolTransceiver
import io.particle.mesh.setup.flow.Clearable
import io.particle.mesh.setup.flow.FlowManager
import mu.KotlinLogging


class DeviceModule(
    private val flowManager: FlowManager,
    private val firmwareUpdateManager: FirmwareUpdateManager
) : Clearable {

    private val log = KotlinLogging.logger {}

    private var hasLatestFirmware = false

    override fun clearState() {
        hasLatestFirmware = false
    }

    // FIXME: where does this belong?
    suspend fun ensureDeviceIsUsingEligibleFirmware(
        xceiver: ProtocolTransceiver,
        deviceType: ParticleDeviceType
    ) {
        log.info { "ensureDeviceIsUsingEligibleFirmware()" }

        if (hasLatestFirmware) {
            log.info { "Already checked device for latest firmware; skipping" }
            return
        }

        val result = firmwareUpdateManager.startUpdateIfNecessary(xceiver, deviceType)
        when(result) {
            FirmwareUpdateResult.HAS_LATEST_FIRMWARE -> {
                hasLatestFirmware = true
                return
            }
            FirmwareUpdateResult.DEVICE_IS_UPDATING -> {
                // weve finished sending the firmware data, and
            }
        }
    }


}