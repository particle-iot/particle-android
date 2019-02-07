package io.particle.mesh.setup.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import io.particle.mesh.setup.flow.MeshDeviceType
import io.particle.mesh.setup.ui.utils.easyDiffUtilCallback
import io.particle.mesh.setup.ui.utils.inflateRow
import io.particle.mesh.setup.ui.utils.localDeviceHasInternetConnection
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_select_device.view.*
import kotlinx.android.synthetic.main.row_select_device.view.*


class SelectDeviceFragment : BaseMeshSetupFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_select_device, container, false)

        // create and populate adapter
        val adapter = MeshDeviceTypesAdapter(this::onItemClicked)
        root.item_list.adapter = adapter
//        adapter.submitList(
//            listOf(
//                DeviceData(
//                    MeshDeviceType.XENON,
//                    R.string.product_description_xenon,
//                    R.drawable.xenon_vector
//                ),
//                DeviceData(
//                    MeshDeviceType.ARGON,
//                    R.string.product_description_argon,
//                    R.drawable.argon_vector
//                ),
//                DeviceData(
//                    MeshDeviceType.BORON,
//                    R.string.product_description_boron,
//                    R.drawable.boron_vector
//                )
//            )
//        )

        if (requireActivity().localDeviceHasInternetConnection()) {
            FlowManagerAccessModel.getViewModel(this).startFlow()
        } else {
            showNoInternetDialog()
        }

        return root
    }

    private fun onItemClicked(deviceData: DeviceData) {
        // FIXME: this should happen via FlowManager
        // check for internet access
//        if (!requireActivity().localDeviceHasInternetConnection()) {
//            showNoInternetDialog()
//            return
//        }
//
//        FlowManagerAccessModel.getViewModel(this).startFlowForDevice(deviceData.deviceType)
    }

    private fun showNoInternetDialog() {
        MaterialDialog.Builder(requireActivity())
            .content("Setup requires an internet connection")
            .positiveText(android.R.string.ok)
            .show()
    }
}


private data class DeviceData(
    val deviceType: MeshDeviceType,
    @StringRes
    val deviceTypeDescription: Int,
    @DrawableRes
    val deviceTypeImage: Int
) {
    @StringRes
    val deviceTypeName: Int = deviceType.toDisplayName()

}


private class DeviceDataHolder(var rowRoot: View) : RecyclerView.ViewHolder(rowRoot) {
    val rowLine1 = rowRoot.row_line_1
    val rowLine2 = rowRoot.row_line_2
    val image = rowRoot.row_image
    val capability2 = rowRoot.p_selectdevice_capability2
}


private class MeshDeviceTypesAdapter(
    private val onItemClicked: (DeviceData) -> Unit
) : ListAdapter<DeviceData, DeviceDataHolder>(
    easyDiffUtilCallback { deviceData: DeviceData -> deviceData.deviceType }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceDataHolder {
        return DeviceDataHolder(inflateRow(parent, R.layout.row_select_device))
    }

    override fun onBindViewHolder(holder: DeviceDataHolder, position: Int) {
        val item = getItem(position)

        holder.rowRoot.setOnClickListener { onItemClicked(item) }
        holder.image.setImageResource(item.deviceTypeImage)
        holder.rowLine1.setText(item.deviceTypeName)
        holder.rowLine2.setText(item.deviceTypeDescription)

        when (item.deviceType) {
            MeshDeviceType.ARGON -> {
                holder.capability2.setImageResource(R.drawable.p_mesh_ic_capability_wifi)
            }
            MeshDeviceType.BORON -> {
                holder.capability2.setImageResource(R.drawable.p_mesh_ic_capability_cellular)
            }
            MeshDeviceType.XENON -> { /* no-op */ }
        }
    }
}


@StringRes
private fun MeshDeviceType.toDisplayName(): Int {
    return when (this) {
        MeshDeviceType.ARGON -> R.string.product_name_argon
        MeshDeviceType.BORON -> R.string.product_name_boron
        MeshDeviceType.XENON -> R.string.product_name_xenon
    }
}
