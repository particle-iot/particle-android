package io.particle.mesh.setup.flow

import kotlinx.coroutines.*

class Scopes(
    val job: Job = Job(),
    val mainThreadScope: CoroutineScope = CoroutineScope(
        Dispatchers.Main + job
    ),
    val backgroundScope: CoroutineScope = CoroutineScope(
        Dispatchers.Default + job
    )
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

    fun onMain(block: suspend CoroutineScope.() -> Unit): Job {
        return mainThreadScope.launch { block() }
    }

    fun onWorker(block: suspend CoroutineScope.() -> Unit): Job {
        return backgroundScope.launch { block() }
    }

}