package io.particle.particlemesh.common.android.livedata

import android.arch.lifecycle.MutableLiveData
import io.particle.particlemesh.meshsetup.utils.isThisTheMainThread


fun <T> MutableLiveData<T>.setOnMainThread(newValue: T) {
    if (isThisTheMainThread()) {
        this.value = newValue
    } else {
        this.postValue(newValue)
    }
}