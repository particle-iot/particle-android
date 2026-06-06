package io.particle.android.sdk.ui.devicelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import androidx.annotation.DrawableRes
import androidx.collection.arrayMapOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.snakydesign.livedataextensions.distinctUntilChanged
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.ui.devicelist.OnlineStatusFilter.ALL_SELECTED
import io.particle.android.sdk.ui.devicelist.OnlineStatusFilter.NONE_SELECTED
import io.particle.android.sdk.ui.devicelist.OnlineStatusFilter.OFFLINE_ONLY
import io.particle.android.sdk.ui.devicelist.OnlineStatusFilter.ONLINE_ONLY
import io.particle.sdk.app.R
import io.particle.sdk.app.databinding.FragmentFilterFragmentBinding
import mu.KotlinLogging


class DeviceFilterFragment : Fragment() {

    companion object {
        fun newInstance() = DeviceFilterFragment()
    }

    private var _binding: FragmentFilterFragmentBinding? = null
    private val binding get() = _binding!!

    private val filterViewModel: DeviceFilterViewModel by activityViewModels()
    private val draftDeviceFilter: DraftDeviceFilter by lazy { filterViewModel.draftDeviceFilter }

    private val onlineStatusFilterButtons: Map<CheckedTextView, OnlineStatusFilter> by lazy {
        arrayMapOf(
            binding.actionFilterOnlineStatusOnline to OnlineStatusFilter.ONLINE_ONLY,
            binding.actionFilterOnlineStatusOffline to OnlineStatusFilter.OFFLINE_ONLY
        )
    }
    private val deviceTypeFilterbuttons: Map<DeviceTypeFilter, DeviceTypeFilterButton> by lazy {
        arrayMapOf(
            DeviceTypeFilter.BORON to binding.actionFilterDeviceTypeBoron,
            DeviceTypeFilter.ELECTRON to binding.actionFilterDeviceTypeElectron,
            DeviceTypeFilter.ARGON to binding.actionFilterDeviceTypeArgon,
            DeviceTypeFilter.PHOTON to binding.actionFilterDeviceTypePhoton,
            DeviceTypeFilter.XENON to binding.actionFilterDeviceTypeXenon,
            DeviceTypeFilter.OTHER to binding.actionFilterDeviceTypeOther
        )
    }

    private var setUpRadioGroupListener = false

    private val log = KotlinLogging.logger {}

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        draftDeviceFilter.updateFromLiveConfig()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFilterFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.actionReset.setOnClickListener { filterViewModel.draftDeviceFilter.resetConfig() }

        binding.pActionBack.setOnClickListener {
            draftDeviceFilter.updateFromLiveConfig()
            closeFilterView()
        }

        for ((v, onlineStatus) in onlineStatusFilterButtons.entries) {
            v.setOnClickListener { draftDeviceFilter.updateOnlineStatus(onlineStatus) }
        }

        for ((deviceType, v) in deviceTypeFilterbuttons.entries) {
            setUpDeviceTypeFilter(v, deviceType)
        }

        binding.actionFilterDeviceList.setOnClickListener {
            draftDeviceFilter.commitDraftConfig()
            closeFilterView()
        }
        draftDeviceFilter.filteredDeviceListLD.observe(this, Observer { updateCommitButton(it!!) })

        draftDeviceFilter.deviceListViewConfigLD
            .distinctUntilChanged()
            .observe(
                viewLifecycleOwner,
                Observer { setUiFromConfig(it) }
            )
    }

    private fun setUpDeviceTypeFilter(view: DeviceTypeFilterButton, filter: DeviceTypeFilter) {
        view.filter = filter
        view.setOnClickListener {
            val isChecked = !view.isChecked // set from the last time the UI updated from the model

            if (isChecked) {
                draftDeviceFilter.includeDeviceInDeviceTypeFilter(filter)
            } else {
                draftDeviceFilter.removeDeviceFromDeviceTypeFilter(filter)
            }
        }
    }

    private fun closeFilterView() {
        requireActivity().supportFragmentManager.popBackStack()
    }

    @Suppress("BooleanLiteralArgument") // ignore the Pair booleans below; it's fine in this case
    private fun setUiFromConfig(config: DeviceListViewConfig) {
        log.info { "setUiFromConfig(): config=$config" }

        fun updateCheckedButton(checkedTextView: CheckedTextView, checked: Boolean) {
            @DrawableRes val bg = if (checked) {
                R.drawable.device_filter_button_background_selected
            } else {
                R.drawable.device_filter_button_background_unselected
            }
            checkedTextView.setBackgroundResource(bg)
            checkedTextView.isChecked = checked
        }


        // SORTING
        val sortButtonId = when (config.sortCriteria) {
            SortCriteria.ONLINE_STATUS -> R.id.action_sort_by_online_status
            SortCriteria.DEVICE_TYPE -> R.id.action_sort_by_device_type
            SortCriteria.NAME -> R.id.action_sort_by_name
            SortCriteria.LAST_HEARD -> R.id.action_sort_by_last_heard
        }
        binding.sortByRadiogroup.check(sortButtonId)


        // ONLINE/OFFLINE STATUS FILTER
        val (onlineChecked, offlineChecked) = when (config.onlineStatusFilter) {
            ALL_SELECTED -> Pair(true, true)
            NONE_SELECTED -> Pair(false, false)
            ONLINE_ONLY -> Pair(true, false)
            OFFLINE_ONLY -> Pair(false, true)
        }

        updateCheckedButton(binding.actionFilterOnlineStatusOnline, onlineChecked)
        updateCheckedButton(binding.actionFilterOnlineStatusOffline, offlineChecked)


        // DEVICE TYPE FILTER
        for (deviceTypeFilter in DeviceTypeFilter.values()) {
            val shouldBeChecked = config.deviceTypeFilters.contains(deviceTypeFilter)
            val button = deviceTypeFilterbuttons[deviceTypeFilter]
            button?.isChecked = shouldBeChecked
        }

        // set this last to avoid some observer loops
        if (!setUpRadioGroupListener) {
            binding.sortByRadiogroup.setOnCheckedChangeListener { _, checkedId ->
                val newSortCriteria = when (checkedId) {
                    R.id.action_sort_by_device_type -> SortCriteria.DEVICE_TYPE
                    R.id.action_sort_by_name -> SortCriteria.NAME
                    R.id.action_sort_by_last_heard -> SortCriteria.LAST_HEARD
                    else -> SortCriteria.ONLINE_STATUS
                }
                draftDeviceFilter.updateSort(newSortCriteria)
            }
            setUpRadioGroupListener = true
        }
    }

    private fun updateCommitButton(newFilteredDeviceList: List<ParticleDevice>) {
        val actionLabel = "Show ${newFilteredDeviceList.size} devices"
        binding.actionFilterDeviceList.text = actionLabel
    }

}
