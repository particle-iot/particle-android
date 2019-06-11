package io.particle.mesh.setup.flow.setupsteps

import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.mesh.setup.flow.*
import io.particle.mesh.setup.flow.ExceptionType.ERROR_FATAL
import io.particle.mesh.setup.flow.SIM_ACTION_MAX_RETRY_COUNT
import io.particle.mesh.setup.flow.context.SetupContexts
import kotlinx.coroutines.delay
import mu.KotlinLogging
import retrofit.RetrofitError
import java.net.SocketTimeoutException


class StepUnpauseSim(
    private val cloud: ParticleCloud,
    private val flowUi: FlowUiDelegate
) : MeshSetupStep() {

    private val log = KotlinLogging.logger {}

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        log.info { "Unpausing SIM with ICCID=${ctxs.targetDevice.iccid}" }

        flowUi.showGlobalProgressSpinner(true)

        retrySimAction {
            cloud.unpauseSim(
                ctxs.targetDevice.iccid!!,
                ctxs.cellular.newSelectedDataLimitLD.value!!
            )
        }
    }

    override fun wrapException(cause: Exception): Exception {
        return FailedToChangeSimDataLimitException(cause)
    }
}
