package io.particle.mesh.common.android.livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.particle.mesh.setup.utils.isThisTheMainThread



fun <T> liveDataWithInitialValue(initialValue: T): MutableLiveData<T> {
    val ld = MutableLiveData<T>()
    ld.value = initialValue
    return ld
}


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
