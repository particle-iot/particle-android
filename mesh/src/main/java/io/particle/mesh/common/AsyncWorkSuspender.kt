package io.particle.mesh.common

import kotlinx.coroutines.CompletableDeferred


fun <T> asyncSuspender(
        startAsyncWorkFunc: ((T) -> Unit) -> Unit,
        beforeAwaitFunc: (() -> Unit)? = null,
        cleanUpFunc: (() -> Unit)? = null
): AsyncWorkSuspender<T> {
    return object : AsyncWorkSuspender<T>() {

        override fun startAsyncWork(workCompleteCallback: (T) -> Unit) {
            startAsyncWorkFunc(workCompleteCallback)
        }

        override fun beforeAwait() {
            if (beforeAwaitFunc != null) {
                beforeAwaitFunc()
            }
        }

        override fun cleanUp() {
            if (cleanUpFunc != null) {
                cleanUpFunc()
            }
        }
    }
}


abstract class AsyncWorkSuspender<T> {

    private var deferred: CompletableDeferred<T> = CompletableDeferred()

    protected abstract fun startAsyncWork(workCompleteCallback: (T) -> Unit)

    protected open fun beforeAwait() {
        // To be optionally implemented by subclasses
    }

    protected open fun cleanUp() {
        // To be implemented by subclasses
    }

    suspend fun awaitResult(): T {
        beforeAwait()
        val result = awaitCondition()
        cleanUp()
        return result
    }

    private suspend fun awaitCondition(): T {
        val originalDeferred = deferred
        try {
            startAsyncWork {
                originalDeferred.complete(it)
            }
        } catch (ex: Exception) {
            originalDeferred.completeExceptionally(ex)
        } finally {
            deferred = CompletableDeferred()
        }

        return originalDeferred.await()
    }

}
