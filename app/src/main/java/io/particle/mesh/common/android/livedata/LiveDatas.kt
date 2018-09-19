package io.particle.mesh.common.android.livedata

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import io.particle.mesh.setup.utils.isThisTheMainThread


fun <T> MutableLiveData<T>.setOnMainThread(newValue: T) {
    if (isThisTheMainThread()) {
        this.value = newValue
    } else {
        this.postValue(newValue)
    }
}


fun <T> LiveData<T>.castAndSetOnMainThread(newValue: T) {
    (this as MutableLiveData).setOnMainThread(newValue)
}


fun <T> LiveData<T>.castAndPost(value: T?) {
    (this as MutableLiveData).postValue(value)
}
