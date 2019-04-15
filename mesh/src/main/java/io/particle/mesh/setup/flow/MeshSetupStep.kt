package io.particle.mesh.setup.flow

import androidx.annotation.WorkerThread
import io.particle.mesh.setup.flow.context.SetupContexts
import kotlinx.coroutines.*
import mu.KotlinLogging



class Scopes(
    val job: Job = Job(),
    val mainThreadScope: CoroutineScope = CoroutineScope(Dispatchers.Main + job),
    val backgroundScope: CoroutineScope = CoroutineScope(Dispatchers.Default + job)
) {

    fun cancelAll() {
        job.cancel()
    }

    suspend fun <T> withMain(block: suspend CoroutineScope.() -> T): T {
        return withContext(mainThreadScope.coroutineContext) {
            block()
        }
    }

    suspend fun <T> withWorker(block: suspend CoroutineScope.() -> T): T {
        return withContext(backgroundScope.coroutineContext) {
            block()
        }
    }

    fun onMain(block: suspend CoroutineScope.() -> Unit) {
        mainThreadScope.launch { block() }
    }

    fun onWorker(block: suspend CoroutineScope.() -> Unit) {
        backgroundScope.launch { block() }
    }

}



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
