package io.particle.android.sdk.ui.devicelist

import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ARGON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.A_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.B5_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.BORON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.B_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ELECTRON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.P1
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.PHOTON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.XENON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.X_SOM
import io.particle.android.sdk.ui.devicelist.OnlineStatusFilter.ALL_SELECTED
import io.particle.android.sdk.ui.devicelist.OnlineStatusFilter.NONE_SELECTED
import io.particle.android.sdk.ui.devicelist.OnlineStatusFilter.OFFLINE_ONLY
import io.particle.android.sdk.ui.devicelist.OnlineStatusFilter.ONLINE_ONLY
import io.particle.android.sdk.ui.devicelist.SortCriteria.DEVICE_TYPE
import io.particle.android.sdk.ui.devicelist.SortCriteria.LAST_HEARD
import io.particle.android.sdk.ui.devicelist.SortCriteria.NAME
import io.particle.android.sdk.ui.devicelist.SortCriteria.ONLINE_STATUS
import mu.KotlinLogging


enum class SortCriteria {
    ONLINE_STATUS,
    DEVICE_TYPE,
    NAME,
    LAST_HEARD
}

enum class OnlineStatusFilter {
    ALL_SELECTED,
    NONE_SELECTED,
    ONLINE_ONLY,
    OFFLINE_ONLY
}

enum class DeviceTypeFilter {
    BORON,    // also B SoM/B5 SoM
    ELECTRON, // also E SoM
    ARGON,    // also A SoM
    PHOTON,   // also P1
    XENON,    // also X SoM
    OTHER
}


data class DeviceListViewConfig(
    val sortCriteria: SortCriteria,
    val onlineStatusFilter: OnlineStatusFilter,
    val deviceTypeFilters: Set<DeviceTypeFilter>,
    val deviceNameQueryString: String? = null
)


private val log = KotlinLogging.logger {}


internal fun sortAndFilterDeviceList(
    devices: List<ParticleDevice>,
    config: DeviceListViewConfig
): List<ParticleDevice> {

    val result = devices.asSequence()
        .filter(getOnlineStatusFilter(config.onlineStatusFilter))
        .filter(getDeviceTypeFilter(config.deviceTypeFilters))
        .filter(getDeviceNameFilter(config.deviceNameQueryString))
        .sortedBy { it.name }
        .sortedWith(getSortingComparator(config.sortCriteria))
        .toList()

    log.info { "Transformed list of ${devices.size} devices down to ${result.size} using config=$config" }

    return result
}

private fun getSortingComparator(criteria: SortCriteria): Comparator<ParticleDevice> {
    return when (criteria) {
        ONLINE_STATUS -> compareByDescending { it.isConnected }
        DEVICE_TYPE -> compareBy(
            { it.deviceType.toDeviceTypeFilter().ordinal },
            { it.deviceType.toString() }
        )
        NAME -> compareBy { it.name }
        LAST_HEARD -> compareByDescending { it.lastHeard }
    }
}

private fun getOnlineStatusFilter(filter: OnlineStatusFilter): (ParticleDevice) -> Boolean {
    return when (filter) {
        ALL_SELECTED,
        NONE_SELECTED -> { _: ParticleDevice -> true }
        ONLINE_ONLY -> { d: ParticleDevice -> d.isConnected }
        OFFLINE_ONLY -> { d: ParticleDevice -> !d.isConnected }
    }
}

private fun ParticleDeviceType?.toDeviceTypeFilter(): DeviceTypeFilter {
    return when (this) {
        A_SOM,
        ARGON -> DeviceTypeFilter.ARGON

        B_SOM,
        B5_SOM,
        BORON -> DeviceTypeFilter.BORON

        PHOTON,
        P1 -> DeviceTypeFilter.PHOTON

        ELECTRON -> DeviceTypeFilter.ELECTRON

        X_SOM,
        XENON -> DeviceTypeFilter.XENON

        else -> DeviceTypeFilter.OTHER
    }
}


private fun getDeviceTypeFilter(filters: Set<DeviceTypeFilter>): (ParticleDevice) -> Boolean {
    if (filters.isEmpty()) {
        return { true }
    }
    return { d: ParticleDevice ->
        filters.contains(d.deviceType.toDeviceTypeFilter())
    }
}

private fun getDeviceNameFilter(query: String?): (ParticleDevice) -> Boolean {
    if (query == null) {
        return { _: ParticleDevice -> true }
    } else {
        return { d: ParticleDevice -> d.name.contains(query, ignoreCase = true) }
    }
}
