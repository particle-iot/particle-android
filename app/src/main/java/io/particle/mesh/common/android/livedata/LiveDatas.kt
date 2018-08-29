package io.particle.mesh.common.android.livedata

import android.arch.lifecycle.MutableLiveData
import io.particle.mesh.setup.utils.isThisTheMainThread


fun <T> MutableLiveData<T>.setOnMainThread(newValue: T) {
    if (isThisTheMainThread()) {
        this.value = newValue
    } else {
        this.postValue(newValue)
    }
}