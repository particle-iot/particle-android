package io.particle.mesh.common.android.livedata

import androidx.lifecycle.LiveData

/**
 * A LiveData which has no data yet, for use with things like switchMap.
 */
class AbsentLiveData<T> : LiveData<T?>() {

    init {
        postValue(null)
    }
}