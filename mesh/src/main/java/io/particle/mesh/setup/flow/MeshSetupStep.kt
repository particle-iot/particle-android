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
            log.info { "RUNNING STEP COMPLETED: ${this.javaClass.simpleName}" }
        } catch (ex: Exception) {
            log.info { "RUNNING STEP COMPLETED WITH EXCEPTION: ${this.javaClass.simpleName}" }
            throw wrapException(ex)
        }
        // TODO: any cleanup we need to do here?
    }

    /** Optionally wrap the given exception in another exception specific to this Step */
    open fun wrapException(cause: Exception): Exception {
        return cause
    }

}
