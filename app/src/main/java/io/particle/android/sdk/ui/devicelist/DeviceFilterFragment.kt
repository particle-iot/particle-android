package io.particle.android.sdk.ui.devicelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.snakydesign.livedataextensions.distinctUntilChanged
import com.snakydesign.livedataextensions.nonNull
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_filter_fragment.*
import mu.KotlinLogging


class DeviceFilterFragment : Fragment() {

    companion object {
        fun newInstance() = DeviceFilterFragment()
    }

    private val filterViewModel: DeviceFilterViewModel by activityViewModels()

    private val log = KotlinLogging.logger {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_filter_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUiFromConfig(filterViewModel.deviceListViewConfigLD.value!!)

        filterViewModel.deviceListViewConfigLD
            .nonNull()
            .distinctUntilChanged()
            .observe(
                viewLifecycleOwner,
                Observer { setUiFromConfig(it) }
            )

        action_close.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        action_filter_device_list.setOnClickListener {
            // TODO!
        }

        action_reset.setOnClickListener { filterViewModel.resetConfig() }

        sort_by_radiogroup.setOnCheckedChangeListener { _, checkedId ->
            val newSortCriteria = when (checkedId) {
                R.id.action_sort_by_device_type -> SortCriteria.DEVICE_TYPE
                R.id.action_sort_by_name -> SortCriteria.NAME
                R.id.action_sort_by_last_heard -> SortCriteria.LAST_HEARD
                else -> SortCriteria.ONLINE_STATUS
            }
            filterViewModel.updateSort(newSortCriteria)
        }
    }

    private fun setUiFromConfig(config: DeviceListViewConfig) {
        log.info { "setUiFromConfig(): config=$config" }

        // SORTING
        val sortButtonId = when (config.sortCriteria) {
            SortCriteria.ONLINE_STATUS -> R.id.action_sort_by_online_status
            SortCriteria.DEVICE_TYPE -> R.id.action_sort_by_device_type
            SortCriteria.NAME -> R.id.action_sort_by_name
            SortCriteria.LAST_HEARD -> R.id.action_sort_by_last_heard
        }
        sort_by_radiogroup.check(sortButtonId)


        // ONLINE STATUS FILTER
        // TODO!

        // DEVICE TYPE FILTER
        // TODO!

        // ACTION BUTTON
        // FIXME: this is wrong -- this needs to be set using a *temp config* so the user has the
        // option to just back/"X" out of the filter screen and not change anything
        val filteredList = sortAndFilterDeviceList(filterViewModel.fullDeviceListLD.value!!, config)
        val actionLabel = "Show ${filteredList.size} devices"
        action_filter_device_list.text = actionLabel
    }

}
