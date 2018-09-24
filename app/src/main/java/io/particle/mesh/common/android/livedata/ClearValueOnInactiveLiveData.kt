package io.particle.mesh.common.android.livedata

import androidx.lifecycle.MutableLiveData
import mu.KotlinLogging

class ClearValueOnInactiveLiveData<T> : MutableLiveData<T>() {

    private val log = KotlinLogging.logger {}

    override fun onInactive() {
        super.onInactive()
        log.info { "LD deactivated, clearing value" }
        this.value = null
    }

}