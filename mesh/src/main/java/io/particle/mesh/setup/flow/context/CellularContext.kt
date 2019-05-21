package io.particle.mesh.setup.flow.context

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.particle.android.sdk.cloud.models.ParticleSim
import io.particle.mesh.common.android.livedata.castAndPost
import io.particle.mesh.common.logged
import io.particle.mesh.setup.flow.Clearable
import mu.KotlinLogging


class CellularContext : Clearable {

    private val log = KotlinLogging.logger {}

    val newSelectedDataLimitLD: LiveData<Int?> = MutableLiveData()
    val changeSimStatusButtonClickedLD: LiveData<Boolean?> = MutableLiveData()

    var connectingToCloudUiShown by log.logged(false)
    var popOwnBackStackOnSelectingDataLimit by log.logged(false)


    override fun clearState() {
        log.info { "clearState()" }

        val liveDatas = listOf(
            newSelectedDataLimitLD,
            changeSimStatusButtonClickedLD
        )
        for (ld in liveDatas) {
            ld.castAndPost(null)
        }

        connectingToCloudUiShown = false
        popOwnBackStackOnSelectingDataLimit = false
    }

    fun updateNewSelectedDataLimit(newLimit: Int) {
        log.info { "updateNewSelectedDataLimit(): $newLimit" }
        newSelectedDataLimitLD.castAndPost(newLimit)
    }

    fun updateChangeSimStatusButtonClicked(clicked: Boolean) {
        log.info { "updateChangeSimStatusButtonClicked(): $clicked" }
        changeSimStatusButtonClickedLD.castAndPost(clicked)
    }
}
