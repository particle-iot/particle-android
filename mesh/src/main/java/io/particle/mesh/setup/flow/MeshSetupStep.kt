package io.particle.mesh.setup.flow

import androidx.annotation.WorkerThread
import io.particle.mesh.setup.flow.context.SetupContexts
import mu.KotlinLogging


abstract class MeshSetupStep {

    private val log = KotlinLogging.logger {}

    @Throws(MeshSetupFlowException::class)
    @WorkerThread
    protected abstract suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes)

    @Throws(MeshSetupFlowException::class)
    @WorkerThread
    suspend fun runStep(contexts: SetupContexts, scopes: Scopes) {
        log.info { "RUNNING STEP: ${this.javaClass.simpleName}" }
        try {
            doRunStep(contexts, scopes)
        } catch (ex: Exception) {
            throw wrapException(ex)
        }
        // TODO: any cleanup we need to do here?
    }

    /** Optionally wrap the given exception in another exception specific to this Step */
    open fun wrapException(cause: Exception): Exception {
        return cause
    }

}
