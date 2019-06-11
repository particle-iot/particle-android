package io.particle.mesh.setup.flow.setupsteps

import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.mesh.common.Result
import io.particle.mesh.common.Result.Absent
import io.particle.mesh.common.Result.Error
import io.particle.mesh.common.Result.Present
import io.particle.mesh.setup.connection.ResultCode
import io.particle.mesh.setup.connection.ResultCode.NOT_ALLOWED
import io.particle.mesh.setup.connection.ResultCode.NOT_FOUND
import io.particle.mesh.setup.connection.ResultCode.TIMEOUT
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.flow.context.SetupContexts
import io.particle.mesh.setup.flow.meshsetup.MeshNetworkToJoin
import kotlinx.coroutines.delay


class StepJoinSelectedNetwork(private val cloud: ParticleCloud) : MeshSetupStep() {

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.mesh.targetJoinedSuccessfully) {
            return
        }

        val joiner = ctxs.requireTargetXceiver()
        val commish = ctxs.requireCommissionerXceiver()

        // no need to send auth msg here; we already authenticated when the password was collected

        commish.sendStartCommissioner().localThrowOnErrorOrAbsent()
        ctxs.mesh.updateCommissionerStarted(true)

        val networkToJoin = when (val toJoinWrapper = ctxs.mesh.meshNetworkToJoinLD.value!!) {
            is MeshNetworkToJoin.SelectedNetwork -> toJoinWrapper.networkToJoin
            is MeshNetworkToJoin.CreateNewNetwork -> throw IllegalStateException()
        }
        val prepJoinerData = joiner.sendPrepareJoiner(networkToJoin).localThrowOnErrorOrAbsent()
        commish.sendAddJoiner(prepJoinerData.eui64, prepJoinerData.password)
            .localThrowOnErrorOrAbsent()

        // value here recommended by Sergey
        delay(10000)

        joiner.sendJoinNetwork().localThrowOnErrorOrAbsent()
        ctxs.mesh.updateTargetJoinedMeshNetwork(true)

        try {
            cloud.addDeviceToMeshNetwork(
                ctxs.targetDevice.deviceId!!,
                networkToJoin.networkId
            )
        } catch (ex: Exception) {
            throw UnableToJoinNetworkException(ex)
        }

        ctxs.mesh.targetJoinedSuccessfully = true
        // let the success UI show for a moment
        delay(2000)
    }

}


private inline fun <reified T> Result<T, ResultCode>.localThrowOnErrorOrAbsent(): T {
    return when (this) {
        is Present -> this.value
        is Absent -> throw MeshSetupFlowException(message = "Absent result on request for ${T::class}")
        is Error -> {
            when (this.error) {
                NOT_ALLOWED -> throw DeviceIsNotAllowedToJoinNetworkException()
                TIMEOUT -> throw DeviceTimeoutWhileJoiningNetworkException()
                NOT_FOUND -> throw DeviceIsUnableToFindNetworkToJoinException()
                else -> throw MeshSetupFlowException(
                    message = "Error result on request for ${T::class}. Error=${this.error}"
                )
            }
        }
    }
}
