package io.particle.mesh.common.android.livedata

import androidx.annotation.AnyThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.annotation.CallSuper
import androidx.annotation.WorkerThread
import io.particle.mesh.common.AsyncWorkSuspender
import io.particle.mesh.common.android.SimpleLifecycleOwner
import io.particle.mesh.setup.flow.Scopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging


@AnyThread
fun <T> liveDataSuspender(
    @AnyThread buildLiveDataFunc: () -> LiveData<T>,
    @AnyThread beforeAwaitFunc: (() -> Unit)? = null,
    @AnyThread cleanUpFunc: (() -> Unit)? = null
): LiveDataSuspender<T> {
    return object : LiveDataSuspender<T>() {

        override fun buildLiveData(): LiveData<T> {
            return buildLiveDataFunc()
        }

        override fun beforeAwait() {
            super.beforeAwait()
            beforeAwaitFunc?.run { beforeAwaitFunc() }
        }

        override fun cleanUp() {
            super.cleanUp()
            cleanUpFunc?.run { cleanUpFunc() }
        }

    }
}


suspend fun <T> LiveData<T>.runBlockOnUiThreadAndAwaitUpdate(toRun: () -> Unit) {
    val suspender = liveDataSuspender({ this })
    withContext(Dispatchers.Main) {
        toRun()
        suspender.awaitResult()
    }
}


suspend fun <T> LiveData<T>.runBlockOnUiThreadAndAwaitUpdate(
    scopes: Scopes,
    block: () -> Unit
): T? {
    val suspender = liveDataSuspender({ this })
    return scopes.withMain {
        block()
        suspender.awaitResult()
    }
}


suspend fun <T> LiveData<T>.awaitUpdate(scopes: Scopes): T? {
    val suspender = liveDataSuspender({ this })
    return scopes.withMain {
        suspender.awaitResult()
    }
}



@WorkerThread
suspend fun <T> LiveData<T?>.nonNull(s: Scopes): LiveData<T?> {
    return s.withMain { this@nonNull.nonNull() }
}


abstract class LiveDataSuspender<T> : AsyncWorkSuspender<T?>() {

    private val lifecycleOwner = SimpleLifecycleOwner()
    private val liveData: LiveData<T> by lazy { buildLiveData() }

    private val log = KotlinLogging.logger {}

    abstract fun buildLiveData(): LiveData<T>

    @CallSuper
    override fun beforeAwait() {
        log.info { "beforeAwait()" }
        lifecycleOwner.setNewState(Lifecycle.State.RESUMED)
    }

    @CallSuper
    override fun cleanUp() {
        log.info { "cleanUp()" }
        lifecycleOwner.setNewState(Lifecycle.State.DESTROYED)
        liveData.removeObservers(lifecycleOwner)
    }

    @CallSuper
    override fun startAsyncWork(workCompleteCallback: (T?) -> Unit) {
        log.info { "startAsyncWork()" }
        liveData.observe(lifecycleOwner, Observer { workCompleteCallback(it) })
    }

}