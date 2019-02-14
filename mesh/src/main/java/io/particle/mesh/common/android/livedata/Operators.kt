package io.particle.mesh.common.android.livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.annotation.MainThread
import io.particle.mesh.common.ActionDebouncer
import io.particle.mesh.setup.utils.runOnMainThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference


@MainThread
fun <X, Y> LiveData<X?>.map(func: (X?) -> Y?): LiveData<Y?> = Transformations.map(this, func)


/**
 * Like [map], except [func] is applied in a background coroutine before posting the value
 */
@MainThread
fun <X, Y> LiveData<X?>.mapAsync(func: (X?) -> Y?): LiveData<Y?> {
    val result = MediatorLiveData<Y?>()
    result.addSource(this) { x ->
        GlobalScope.launch(Dispatchers.Default) {
            val transformed = func(x)
            result.postValue(transformed)
        }
    }
    return result
}


@MainThread
fun <X, Y> LiveData<X?>.switchMap(func: (X?) -> LiveData<Y?>): LiveData<Y?> {
    return Transformations.switchMap(this, func)
}


@MainThread
fun <T> LiveData<T?>.filter(predicate: (T?) -> Boolean): LiveData<T?> {
    val result = MediatorLiveData<T?>()

    result.addSource(this) { x ->
        if (predicate(x)) {
            result.value = x
        }
    }
    return result
}


@MainThread
fun <T> LiveData<T?>.nonNull(): LiveData<T?> {
    return this.filter { it != null }
}


@MainThread
fun <T> LiveData<T?>.first(predicate: (T?) -> Boolean): LiveData<T?> {
    val result = MediatorLiveData<T?>()
    result.addSource(this) { x ->
        if (predicate(x)) {
            result.postValue(x)
            // Unsubscribe after posting the first value
            runOnMainThread { result.removeSource(this@first) }
        }
    }
    return result
}


@MainThread
fun <T> LiveData<T?>.distinct(): LiveData<T?> {
    val distinctLiveData = MediatorLiveData<T?>()
    distinctLiveData.addSource(
            this,
            object : Observer<T?> {

                private var initialized = false
                private var prevValue: T? = null

                override fun onChanged(value: T?) {
                    if (!initialized) {
                        initialized = true
                        prevValue = value
                        distinctLiveData.postValue(prevValue)

                    } else if (prevValue != value) {
                        prevValue = value
                        distinctLiveData.postValue(prevValue)
                    }
                }
            })

    return distinctLiveData
}


/**
 * It's just like [distinct], but it performs the equality check in a background coroutine
 * before posting the value
 */
@MainThread
fun <T> LiveData<T?>.distinctAsync(): LiveData<T?> {
    val distinctLiveData = MediatorLiveData<T?>()
    distinctLiveData.addSource(
            this,

            object : Observer<T?> {

                private var lock = Any()
                private var initialized = false
                private var previousValue: T? = null

                override fun onChanged(newValue: T?) {
                    GlobalScope.launch(Dispatchers.Default) {
                        synchronized(lock) {
                            if (!initialized) {
                                initialized = true
                                previousValue = newValue
                                distinctLiveData.postValue(previousValue)

                            } else if (previousValue != newValue) {
                                previousValue = newValue
                                distinctLiveData.postValue(previousValue)
                            }
                        }
                    }
                }
            })

    return distinctLiveData
}


@MainThread
fun <T> LiveData<T?>.debounce(debouncingTimeMillis: Long): LiveData<T?> {
    // create atomicrefs due to the circular references here. (ugh)
    val debouncerRef = AtomicReference<ActionDebouncer>()
    val nextValueRef = AtomicReference<T?>()

    val distinctLD = object : MediatorLiveData<T?>() {
        override fun onInactive() {
            super.onInactive()
            debouncerRef.get()?.cancelScheduledAction()
        }
    }

    debouncerRef.set(ActionDebouncer(debouncingTimeMillis) {
        distinctLD.postValue(nextValueRef.get())
    })

    distinctLD.addSource(this) { value ->
        nextValueRef.set(value)
        debouncerRef.get()!!.executeDebouncedAction()
    }

    return distinctLD
}
