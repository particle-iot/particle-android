package io.particle.mesh.ui.controlpanel


import android.R.color
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.particle.android.common.easyDiffUtilCallback
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.setup.utils.safeToast
import io.particle.mesh.ui.R
import io.particle.mesh.ui.TitleBarOptions
import io.particle.mesh.ui.controlpanel.DataLimitAdapter.DataLimitHolder
import io.particle.mesh.ui.databinding.ControlpanelRowDataLimitBinding
import io.particle.mesh.ui.databinding.FragmentControlPanelCellularDataLimitBinding
import io.particle.mesh.ui.inflateFragment
import io.particle.mesh.ui.inflateRow
import kotlin.math.roundToInt


class ControlPanelCellularDataLimitFragment : BaseControlPanelFragment() {

    override val titleBarOptions = TitleBarOptions(
        R.string.p_controlpanel_datalimit_title,
        showBackButton = true
    )

    private val limits: List<Int> = listOf(1, 2, 3, 5, 10, 20, 50, 100, 200, 500)
    private lateinit var adapter: DataLimitAdapter

    private var selectedLimit: Int? = null

    private var _binding: FragmentControlPanelCellularDataLimitBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = container?.inflateFragment(R.layout.fragment_control_panel_cellular_data_limit)
        _binding = root?.let { FragmentControlPanelCellularDataLimitBinding.bind(it) }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        adapter = DataLimitAdapter(::onDataLimitItemClicked, activity)
        adapter.currentDataLimit = flowUiListener.targetDevice.sim?.monthlyDataRateLimitInMBs
        adapter.currentDataUsage = flowUiListener.targetDevice.dataUsedInMB?.roundToInt()

        binding.limitsList.adapter = adapter
        adapter.submitList(limits)

        binding.actionChangeDataLimit.setOnClickListener { onChangeLimitClicked() }
    }

    private fun onDataLimitItemClicked(item: Int) {
        selectedLimit = item
        binding.actionChangeDataLimit.isEnabled = true
    }

    private fun onChangeLimitClicked() {
        flowUiListener?.cellular?.updateNewSelectedDataLimit(selectedLimit!!)
        // FIXME: this is a hack.  Look for a cleaner approach
        if (flowUiListener?.cellular?.popOwnBackStackOnSelectingDataLimit == true) {
            findNavController().popBackStack()
        }
    }

}


private class DataLimitAdapter(
    private val itemClickedCallback: (Int) -> Unit,
    private val everythingNeedsAContext: Context
) : ListAdapter<Int, DataLimitHolder>(
    easyDiffUtilCallback { item: Int -> item }
) {

    class DataLimitHolder(val root: View) : ViewHolder(root) {
        private val itemBinding = ControlpanelRowDataLimitBinding.bind(root)
        val limitValue: TextView = itemBinding.limitValue
        val checkbox: ImageView = itemBinding.selectedCheckmark
    }

    var currentDataLimit: Int? = null
    var currentDataUsage: Int? = null
    var selectedDataLimit: Int? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataLimitHolder {
        return DataLimitHolder(parent.inflateRow(R.layout.controlpanel_row_data_limit))
    }

    override fun onBindViewHolder(holder: DataLimitHolder, position: Int) {
        val item = getItem(position)

        @SuppressLint("SetTextI18n")
        holder.limitValue.text = "${item}MB"

        when (item) {
            currentDataLimit -> {
                holder.checkbox.isVisible = true
                holder.checkbox.setImageResource(R.drawable.ic_check_gray_24dp)
            }
            selectedDataLimit -> {
                holder.checkbox.isVisible = true
                holder.checkbox.setImageResource(R.drawable.ic_check_cyan_24dp)
            }
            else -> holder.checkbox.isVisible = false
        }

        val enableItem = item > (currentDataUsage?: 0)
        holder.root.isEnabled = enableItem
        val textColor = ContextCompat.getColor(
            everythingNeedsAContext,
            if (enableItem) color.black else R.color.p_faded_gray
        )
        holder.limitValue.setTextColor(textColor)
        holder.root.setOnClickListener { onItemClicked(item) }
    }

    private fun onItemClicked(item: Int) {
        selectedDataLimit = item
        notifyDataSetChanged()
        itemClickedCallback(item)
    }

}
