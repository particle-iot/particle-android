package io.particle.mesh.setup.flow.setupsteps

import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.utils.pass
import io.particle.mesh.R.string
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.setup.SerialNumber
import io.particle.mesh.setup.fetchBarcodeData
import io.particle.mesh.setup.flow.DialogResult.NEGATIVE
import io.particle.mesh.setup.flow.DialogResult.POSITIVE
import io.particle.mesh.setup.flow.DialogSpec.ResDialogSpec
import io.particle.mesh.setup.flow.DialogSpec.StringDialogSpec
import io.particle.mesh.setup.flow.FlowUiDelegate
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.TerminateFlowAndStartControlPanelException
import io.particle.mesh.setup.flow.context.SetupContexts
import mu.KotlinLogging


class StepCheckShouldSwitchToControlPanel(
    private val cloud: ParticleCloud,
    private val flowUi: FlowUiDelegate
) : MeshSetupStep() {

    private val log = KotlinLogging.logger {}

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.targetDevice.ownershipCheckedForSwitchingToControlPanel) {
            return // already done; bail.
        }

        flowUi.showGlobalProgressSpinner(true)
        val devices = cloud.getDevices()
        val serial = ctxs.targetDevice.barcode.value!!.serialNumber
        val ownedDevice = devices.firstOrNull {
            val otherSerial = it.serialNumber?.let { ser -> SerialNumber(ser.toUpperCase()) }
            otherSerial == serial
        }

        ctxs.targetDevice.ownershipCheckedForSwitchingToControlPanel = true

        if (ownedDevice == null) {
            log.info { "Device is NOT owned by this user" }
            return  // nope, not owned, carry on
        }

        log.info { "Device IS owned by this user; prompting to go to Control Panel" }

        // show dialog and block
        val dialogResult = flowUi.dialogTool.dialogResultLD
            .nonNull(scopes)
            .runBlockOnUiThreadAndAwaitUpdate(scopes) {
                flowUi.dialogTool.newDialogRequest(
                    StringDialogSpec(
                        title = "Switch to Control Panel?",
                        text = "This device already belongs to your account.  Would you like to " +
                                "switch to Control Panel or continue with guided setup?",
                        positiveText = "Switch to Control Panel",
                        negativeText = "Continue setup"
                    )
                )
            }
        when (dialogResult) {
            NEGATIVE,
            null -> pass  // continue on with setup
            POSITIVE -> {
                throw TerminateFlowAndStartControlPanelException(ownedDevice)
            }
        }

        flowUi.showGlobalProgressSpinner(false)
    }

}
