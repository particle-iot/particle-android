package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.ota.FirmwareUpdateManager
import io.particle.mesh.ota.FirmwareUpdateResult.DEVICE_IS_UPDATING
import io.particle.mesh.ota.FirmwareUpdateResult.HAS_LATEST_FIRMWARE
import io.particle.mesh.setup.flow.FailedToUpdateDeviceOSException
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.FlowUiDelegate
import kotlinx.coroutines.delay
import mu.KotlinLogging


class StepEnsureLatestFirmware(
    private val flowUi: FlowUiDelegate,
    private val firmwareUpdateManager: FirmwareUpdateManager
) : MeshSetupStep() {

    private val log = KotlinLogging.logger {}

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.targetDevice.hasLatestFirmware) {
            log.info { "Already checked device for latest firmware; skipping" }
            return
        }

        val xceiver = ctxs.targetDevice.transceiverLD.value!!
        val deviceType = ctxs.targetDevice.deviceType!!

        val needsUpdate = firmwareUpdateManager.needsUpdate(xceiver, deviceType)
        if (!needsUpdate) {
            log.debug { "No firmware update needed!" }
            ctxs.targetDevice.hasLatestFirmware = true
            return
        }

        ctxs.device.userConsentedToFirmwareUpdateLD
            .nonNull(scopes)
            .runBlockOnUiThreadAndAwaitUpdate(scopes) {
                flowUi.showBleOtaIntroUi()
            }

        ctxs.device.updateBleOtaProgress(0)
        // FIXME: change state to "updating OTA via BLE"
        flowUi.showBleOtaUi()

        val status = try {
            firmwareUpdateManager.startUpdateIfNecessary(xceiver, deviceType) {
                ctxs.device.updateBleOtaProgress(it)
            }
        } catch (ex: Exception) {
            throw FailedToUpdateDeviceOSException(ex)
        }

        when (status) {
            HAS_LATEST_FIRMWARE -> { /* no-op */ }
            DEVICE_IS_UPDATING -> {
                flowUi.showGlobalProgressSpinner(true)
                // there can be a *very* long delay after the FW update.
                // FIXME: don't make this a static value, start a BT scanner
                // to watch for the device again
                delay(12000)
            }
        }

        ctxs.device.firmwareUpdateCount++
    }

}