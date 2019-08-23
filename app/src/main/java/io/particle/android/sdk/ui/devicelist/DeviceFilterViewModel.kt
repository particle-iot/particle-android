package io.particle.android.sdk.ui.devicelist

import android.app.Application
import androidx.lifecycle.*
import com.snakydesign.livedataextensions.distinctUntilChanged
import com.snakydesign.livedataextensions.map
import com.snakydesign.livedataextensions.nonNull
import com.snakydesign.livedataextensions.switchMap
import io.particle.android.sdk.cloud.BroadcastContract
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.ui.devicelist.OnlineStatusFilter.ALL_SELECTED
import io.particle.android.sdk.ui.devicelist.OnlineStatusFilter.NONE_SELECTED
import io.particle.android.sdk.ui.devicelist.OnlineStatusFilter.OFFLINE_ONLY
import io.particle.android.sdk.ui.devicelist.OnlineStatusFilter.ONLINE_ONLY
import io.particle.mesh.common.android.livedata.BroadcastReceiverLD
import io.particle.mesh.common.android.livedata.castAndPost
import io.particle.mesh.common.android.livedata.castAndSetOnMainThread
import io.particle.mesh.setup.flow.Scopes
import mu.KLogger
import mu.KotlinLogging


val defaultDeviceListConfig = DeviceListViewConfig(
    SortCriteria.ONLINE_STATUS,
    OnlineStatusFilter.NONE_SELECTED,
    emptySet()
)


open class DeviceFilter(
    fullDeviceListLD: LiveData<List<ParticleDevice>>
) {

    var filteredDeviceListLD: LiveData<List<ParticleDevice>>
    val deviceListViewConfigLD: MutableLiveData<DeviceListViewConfig> = MutableLiveData()

    protected var currentConfig: DeviceListViewConfig
        get() {
            return _currentConfig
        }
        set(value) {
            if (value == _currentConfig) {
                log.info { "New config matches; doing nothing." }
                return
            }
            log.info { "Changing config to: $value" }
            _currentConfig = value
            deviceListViewConfigLD.castAndPost(_currentConfig)
        }

    private var _currentConfig: DeviceListViewConfig

    protected open val log: KLogger = KotlinLogging.logger {}

    init {
        _currentConfig = defaultDeviceListConfig
        deviceListViewConfigLD.castAndSetOnMainThread(_currentConfig)

        filteredDeviceListLD = deviceListViewConfigLD
            .switchMap {
                fullDeviceListLD
                    .map { sortAndFilterDeviceList(it, currentConfig) }
            }
    }

    fun applyNewConfig(newConfig: DeviceListViewConfig) {
        log.info { "Applying new config: $newConfig" }
        currentConfig = newConfig
    }

    fun resetConfig() {
        applyNewConfig(defaultDeviceListConfig)
    }

}



class DraftDeviceFilter(
    private val deviceFilter: DeviceFilter,
    fullDeviceListLD: LiveData<List<ParticleDevice>>
) : DeviceFilter(fullDeviceListLD) {

    override val log: KLogger = KotlinLogging.logger {}

    fun commitDraftConfig() {
        log.info { "Committing draft config" }
        deviceFilter.applyNewConfig(currentConfig)
    }

    fun updateFromLiveConfig() {
        log.info { "Updating draft to use current live config" }
        currentConfig = deviceFilter.deviceListViewConfigLD.value!!
    }

    fun updateNameQuery(query: String?) {
        log.info { "updateNameQuery(): query=$query" }
        val newQuery = if (query.isNullOrBlank()) null else query
        currentConfig = currentConfig.copy(deviceNameQueryString = newQuery)
    }

    fun updateSort(sortCriteria: SortCriteria) {
        log.info { "updateSort(): sortCriteria=$sortCriteria" }
        currentConfig = currentConfig.copy(sortCriteria = sortCriteria)
    }

    fun updateOnlineStatus(onlineStatusFilter: OnlineStatusFilter) {
        log.info { "updateOnlineStatus(): onlineStatusFilter=$onlineStatusFilter" }

        fun returnOppositeFilter(filter: OnlineStatusFilter): OnlineStatusFilter {
            return when (filter) {
                ALL_SELECTED -> NONE_SELECTED
                NONE_SELECTED -> ALL_SELECTED
                ONLINE_ONLY -> OFFLINE_ONLY
                OFFLINE_ONLY -> ONLINE_ONLY
            }
        }

        val newFilter = when (currentConfig.onlineStatusFilter) {
            ALL_SELECTED -> returnOppositeFilter(onlineStatusFilter)
            NONE_SELECTED -> onlineStatusFilter
            ONLINE_ONLY -> if (onlineStatusFilter == ONLINE_ONLY) {
                NONE_SELECTED
            } else {
                ALL_SELECTED
            }
            OFFLINE_ONLY -> if (onlineStatusFilter == OFFLINE_ONLY) {
                NONE_SELECTED
            } else {
                ALL_SELECTED
            }
        }

        currentConfig = currentConfig.copy(onlineStatusFilter = newFilter)
    }

    fun includeDeviceInDeviceTypeFilter(toInclude: DeviceTypeFilter) {
        log.info { "includeDeviceInDeviceTypeFilter(): toInclude=$toInclude" }
        val newDeviceTypes = currentConfig.deviceTypeFilters.plus(toInclude)
        currentConfig = currentConfig.copy(deviceTypeFilters = newDeviceTypes)
    }

    fun removeDeviceFromDeviceTypeFilter(toRemove: DeviceTypeFilter) {
        log.info { "removeDeviceFromDeviceTypeFilter(): toRemove=$toRemove" }
        val newDeviceTypes = currentConfig.deviceTypeFilters.minus(toRemove)
        currentConfig = currentConfig.copy(deviceTypeFilters = newDeviceTypes)
    }
}


class DeviceFilterViewModel(app: Application) : AndroidViewModel(app) {

    val fullDeviceListLD: LiveData<List<ParticleDevice>> = MutableLiveData()

    val currentDeviceFilter = DeviceFilter(fullDeviceListLD)
    val draftDeviceFilter = DraftDeviceFilter(currentDeviceFilter, fullDeviceListLD)

    private val cloud = ParticleCloudSDK.getCloud()
    private val devicesUpdatedBroadcast: BroadcastReceiverLD<Int>
    private val refreshObserver = Observer<Any?> { refreshDevices() }
    private val scopes = Scopes()

    private val log = KotlinLogging.logger {}

    init {
        // arbitrary value that always changes every time we receive the broadcast, telling us to
        // retrieve the new devices
        var initialValue = 0
        devicesUpdatedBroadcast = BroadcastReceiverLD(
            app,
            BroadcastContract.BROADCAST_DEVICES_UPDATED,
            { ++initialValue },
            true
        )

        devicesUpdatedBroadcast.observeForever(refreshObserver)
    }

    override fun onCleared() {
        super.onCleared()
        scopes.cancelAll()
        devicesUpdatedBroadcast.removeObserver(refreshObserver)
    }

    fun refreshDevices() {
        scopes.onMain { doRefreshDevices() }
    }

    private suspend fun doRefreshDevices() {
        val deviceList = scopes.withWorker {
            cloud.getDevices()
        }
        log.info { "Posting new device list: $deviceList" }
        fullDeviceListLD.castAndPost(deviceList)
        val config = currentDeviceFilter.deviceListViewConfigLD.value!!
        currentDeviceFilter.applyNewConfig(config)
    }

}
