package io.particle.mesh.common.android.livedata

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.support.annotation.CallSuper
import io.particle.mesh.common.AsyncWorkSuspender
import io.particle.mesh.common.android.SimpleLifecycleOwner


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


abstract class LiveDataSuspender<T> : AsyncWorkSuspender<T?>() {

    private val lifecycleOwner = SimpleLifecycleOwner()
    private val liveData: LiveData<T> by lazy { buildLiveData() }

    abstract fun buildLiveData(): LiveData<T>

    @CallSuper
    override fun beforeAwait() {
        lifecycleOwner.setNewState(Lifecycle.State.RESUMED)
    }

    @CallSuper
    override fun cleanUp() {
        lifecycleOwner.setNewState(Lifecycle.State.DESTROYED)
        liveData.removeObservers(lifecycleOwner)
    }

    @CallSuper
    override fun startAsyncWork(workCompleteCallback: (T?) -> Unit) {
        liveData.observe(lifecycleOwner, Observer { workCompleteCallback(it) })
    }

}