package io.particle.mesh.setup.flow

import kotlinx.coroutines.*


class Scopes(
    val job: Job = SupervisorJob(),
    val mainThreadScope: CoroutineScope = CoroutineScope(Dispatchers.Main + job),
    val backgroundScope: CoroutineScope = CoroutineScope(Dispatchers.Default + job)
) {

    fun cancelAll() {
        job.cancel()
    }

    suspend fun <T> withMain(timeout: Long? = null, block: suspend CoroutineScope.() -> T): T {
        return runDeferred(mainThreadScope, timeout, block)
    }

    suspend fun <T> withWorker(timeout: Long? = null, block: suspend CoroutineScope.() -> T): T {
        return runDeferred(backgroundScope, timeout, block)
    }

    fun onMain(block: suspend CoroutineScope.() -> Unit): Job {
        return mainThreadScope.launch { block() }
    }

    fun onWorker(block: suspend CoroutineScope.() -> Unit): Job {
        return backgroundScope.launch { block() }
    }


    private suspend fun <T> runDeferred(
        scope: CoroutineScope,
        timeout: Long? = null,
        block: suspend CoroutineScope.() -> T
    ): T {
        return if (timeout == null) {
            withContext(scope.coroutineContext, block)
        } else {
            deferInContextWithTimeout(scope, timeout, block)
        }
    }

    private suspend fun <T> deferInContextWithTimeout(
        scope: CoroutineScope,
        timeout: Long,
        block: suspend CoroutineScope.() -> T
    ): T {
        return withTimeout(timeout) {
            val deferred: Deferred<T> = scope.async { return@async block() }
            return@withTimeout deferred.await()
        }
    }



}