package io.particle.android.sdk.ui.devicelist

import android.app.Application
import androidx.lifecycle.*
import io.particle.android.sdk.cloud.BroadcastContract
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.mesh.common.android.livedata.BroadcastReceiverLD
import io.particle.mesh.common.android.livedata.castAndPost
import io.particle.mesh.common.android.livedata.castAndSetOnMainThread
import io.particle.mesh.setup.flow.Scopes
import mu.KotlinLogging


class DeviceFilterViewModel(app: Application) : AndroidViewModel(app) {

    val fullDeviceListLD: LiveData<List<ParticleDevice>> = MutableLiveData()
    val filteredDeviceListLD: LiveData<List<ParticleDevice>>

    private val cloud = ParticleCloudSDK.getCloud()
    private val deviceListViewConfigLD: MutableLiveData<DeviceListViewConfig> = MutableLiveData()
    private val devicesUpdatedBroadcast: BroadcastReceiverLD<Int>
    private val refreshObserver = Observer<Any?> { refreshDevices() }
    private val scopes = Scopes()

    private val log = KotlinLogging.logger {}

    init {
        // set initial value on list config LD
        deviceListViewConfigLD.castAndSetOnMainThread(
            DeviceListViewConfig(
                SortCriteria.ONLINE_STATUS,
                OnlineStatusFilter.ALL,
                DeviceTypeFilter.values().toSet()
            )
        )

        filteredDeviceListLD = Transformations.map(fullDeviceListLD) {
            sortAndFilterDeviceList(it, deviceListViewConfigLD.value!!)
        }

        // arbitrary value that always changes every time we receive the broadcast, telling us to
        // retrieve the new devices
        var initialValue = 0
        devicesUpdatedBroadcast = BroadcastReceiverLD(
            app,
            BroadcastContract.BROADCAST_DEVICES_UPDATED,
            { ++initialValue },
            true
        )

        // FIXME: figure out the right way to wire this all up so we don't bother updating this
        // when there are no observers of the device list
        // TIP: the answer is "switchMap" -- look into how we do it in the Bluetooth stack
        devicesUpdatedBroadcast.observeForever(refreshObserver)
        deviceListViewConfigLD.observeForever(refreshObserver)
    }

    override fun onCleared() {
        super.onCleared()
        scopes.cancelAll()
        devicesUpdatedBroadcast.removeObserver(refreshObserver)
        deviceListViewConfigLD.removeObserver(refreshObserver)
    }

    fun refreshDevices() {
        scopes.onMain { doRefreshDevices() }
    }

    fun updateNameQuery(query: String?) {
        val currentConfig = deviceListViewConfigLD.value!!
        updateConfig(
            currentConfig.copy(deviceNameQueryString = query)
        )
    }

    fun updateConfig(listConfig: DeviceListViewConfig) {
        deviceListViewConfigLD.castAndPost(listConfig)
    }

    private suspend fun doRefreshDevices() {
        val deviceList = scopes.withWorker {
            cloud.getDevices()
        }
        log.info { "Posting devices: $deviceList" }
        fullDeviceListLD.castAndPost(deviceList)
    }

}
