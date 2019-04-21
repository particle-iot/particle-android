package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.ota.FirmwareUpdateManager
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.FlowUiDelegate
import mu.KotlinLogging


class StepEnsureLatestFirmware(
    private val flowUi: FlowUiDelegate,
    private val firmwareUpdateManager: FirmwareUpdateManager
) : MeshSetupStep() {

    private val log = KotlinLogging.logger {}

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.targetDevice.hasLatestFirmware) {
            log.info { "Already checked device for latest firmware; skipping" }
            flowUi.showGlobalProgressSpinner(false)
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

        firmwareUpdateManager.startUpdateIfNecessary(xceiver, deviceType) {
            ctxs.device.updateBleOtaProgress(it)
        }

        // FIXME: this probably shouldn't live in the flow, but in the UI
        flowUi.showGlobalProgressSpinner(true)
        ctxs.device.firmwareUpdateCount++
    }

}