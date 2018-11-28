package io.particle.mesh.common.android.livedata

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.annotation.CallSuper
import io.particle.mesh.common.AsyncWorkSuspender
import io.particle.mesh.common.android.SimpleLifecycleOwner
import kotlinx.coroutines.withContext
import mu.KotlinLogging


fun <T> liveDataSuspender(
        buildLiveDataFunc: () -> LiveData<T>,
        beforeAwaitFunc: (() -> Unit)? = null,
        cleanUpFunc: (() -> Unit)? = null
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


suspend fun <T> LiveData<T>.runOnUiThreadAndWaitForUpdate(toRun: () -> Unit) {
    val suspender = liveDataSuspender({ this })
    withContext(UI) {
        toRun()
        suspender.awaitResult()
    }
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