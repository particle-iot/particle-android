package io.particle.mesh.common.android

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.particle.mesh.common.QATool
import io.particle.mesh.setup.flow.Scopes
import kotlinx.coroutines.*


/**
 * Everything necessary to create a mini "DSL" for async operations based coroutines
 * that are scoped to a given [LifecycleOwner]
 *
 * Pulled from: https://hellsoft.se/simple-asynchronous-loading-with-kotlin-coroutines-f26408f97f46
 */


class CoroutineOnDestroyListener(val deferred: Deferred<*>) : DefaultLifecycleObserver {

    override fun onDestroy(owner: LifecycleOwner) {
        if (!deferred.isCancelled) {
            deferred.cancel()
        }
    }
}


class CoroutineOnStopListener(val deferred: Deferred<*>) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        if (!deferred.isCancelled) {
            deferred.cancel()
        }
    }
}


enum class LifecycleCancellationPoint {
    ON_STOP,
    ON_DESTROY
}


/**
 * Creates a lazily started coroutine that runs <code>loader()</code>.
 * The coroutine is automatically cancelled using the CoroutineLifecycleListener.
 */
fun <T> LifecycleOwner.load(
    scopes: Scopes,
    cancelWhen: LifecycleCancellationPoint = LifecycleCancellationPoint.ON_DESTROY,
    loader: () -> T
): Deferred<T> {
    val deferred = scopes.backgroundScope.async(start = CoroutineStart.LAZY, block = {
        loader()
    })

    val listener = when (cancelWhen) {
        LifecycleCancellationPoint.ON_STOP -> CoroutineOnStopListener(deferred)
        LifecycleCancellationPoint.ON_DESTROY -> CoroutineOnDestroyListener(deferred)
    }

    lifecycle.addObserver(listener)

    return deferred
}


/**
 * Extension function on <code>Deferred<T><code> that creates a launches a coroutine which
 * will call <code>await()</code> and pass the returned value to <code>block()</code>.
 */
infix fun <T> Deferred<T>.then(block: (T) -> Unit): Job {
    return GlobalScope.launch {
        try {
            block(this@then.await())
        } catch (ex: Exception) {
            QATool.report(ex)
            throw ex
        }
    }
}

