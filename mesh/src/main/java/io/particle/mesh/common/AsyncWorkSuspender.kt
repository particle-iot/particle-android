package io.particle.mesh.common

import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


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

//    private val deferred: CompletableDeferred<T> = CompletableDeferred()

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
        val result = suspendCoroutine { continuation: Continuation<Result<T, Exception>> ->
            try {
                startAsyncWork {
                    continuation.resume(Result.Present(it))
                }
            } catch (ex: Exception) {
                continuation.resume(Result.Error(ex))
            }
        }

        when(result) {
            is Result.Present -> return result.value
            is Result.Error -> throw result.error
            is Result.Absent -> throw IllegalStateException(
                "Absent result from awaitCondition() in ${this::class.java}??"
            )
        }
    }

}