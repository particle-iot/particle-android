package io.particle.mesh.common.android.livedata

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations

/**
 * A LiveData which has no data yet, for use with things like [Transformations.switchMap].
 */
class AbsentLiveData<T> : LiveData<T?>() {

    init {
        postValue(null)
    }
}