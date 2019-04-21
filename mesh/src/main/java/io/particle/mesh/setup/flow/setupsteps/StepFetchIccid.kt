package io.particle.mesh.setup.flow.setupsteps

import io.particle.mesh.common.Result
import io.particle.mesh.common.truthy
import io.particle.mesh.setup.connection.ResultCode
import io.particle.mesh.setup.flow.MeshSetupFlowException
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import kotlinx.coroutines.delay


class StepFetchIccid : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.targetDevice.iccid.truthy()) {
            return
        }

        val targetXceiver = ctxs.requireTargetXceiver()

        val iccidReply = targetXceiver.sendGetIccId()
        when (iccidReply) {
            is Result.Present -> {
                ctxs.targetDevice.iccid = iccidReply.value.iccid
            }

            is Result.Error -> {
                if (iccidReply.error == ResultCode.INVALID_STATE) {
                    targetXceiver.sendReset()
                    delay(2000)
                    throw MeshSetupFlowException("INVALID_STATE received while getting ICCID; " +
                            "sending reset command and restarting flow"
                    )
                }
                throw MeshSetupFlowException("Error ${iccidReply.error} when retrieving ICCID")
            }

            is Result.Absent -> {
                throw MeshSetupFlowException("Unknown error when retrieving ICCID")
            }
        }
    }

}