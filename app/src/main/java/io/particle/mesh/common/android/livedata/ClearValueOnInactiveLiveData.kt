package io.particle.mesh.common.android.livedata

import android.arch.lifecycle.MutableLiveData

class ClearValueOnInactiveLiveData<T> : MutableLiveData<T>() {

    override fun onInactive() {
        super.onInactive()
        this.value = null
    }

}