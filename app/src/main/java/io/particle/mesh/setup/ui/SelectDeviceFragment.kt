package io.particle.mesh.setup.ui

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.afollestad.materialdialogs.MaterialDialog
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.mesh.setup.ui.utils.easyDiffUtilCallback
import io.particle.mesh.setup.ui.utils.inflateRow
import io.particle.mesh.setup.ui.utils.localDeviceHasInternetConnection
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_select_device.view.*
import kotlinx.android.synthetic.main.row_select_device.view.*


class SelectDeviceFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_select_device, container, false)

        // create and populate adapter
        val adapter = MeshDeviceTypesAdapter(this::onItemClicked)
        root.item_list.adapter = adapter
        adapter.submitList(listOf(
                DeviceData(
                        ParticleDeviceType.XENON,
                        R.string.product_description_xenon,
                        R.drawable.xenon_vector
                ),
                DeviceData(
                        ParticleDeviceType.ARGON,
                        R.string.product_description_argon,
                        R.drawable.argon_vector
                ),
                DeviceData(
                        ParticleDeviceType.BORON,
                        R.string.product_description_boron,
                        R.drawable.boron_vector
                )
        ))

        return root
    }

    private fun onItemClicked(deviceData: DeviceData) {
        // FIXME: this should happen via FlowManager
        // check for internet access
        if (!requireActivity().localDeviceHasInternetConnection()) {
            showNoInternetDialog()
            return
        }

        FlowManagerAccessModel.getViewModel(this).startFlowForDevice(deviceData.deviceType)
    }

    private fun showNoInternetDialog() {
        MaterialDialog.Builder(requireActivity())
                .content("Setup requires an internet connection")
                .positiveText(android.R.string.ok)
                .show()
    }
}


private data class DeviceData(
        val deviceType: ParticleDeviceType,
        @StringRes
        val deviceTypeDescription: Int,
        @DrawableRes
        val deviceTypeImage: Int
) {
    @StringRes
    val deviceTypeName: Int = deviceType.toDisplayName()

}


private class DeviceDataHolder(var rowRoot: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(rowRoot) {
    val rowLine1 = rowRoot.row_line_1
    val rowLine2 = rowRoot.row_line_2
    val image = rowRoot.row_image
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

        holder.image.setImageResource(item.deviceTypeImage)
        holder.rowLine1.setText(item.deviceTypeName)
        holder.rowLine2.setText(item.deviceTypeDescription)

        holder.rowRoot.setOnClickListener { onItemClicked(item) }
    }

}


@StringRes
private fun ParticleDeviceType.toDisplayName(): Int {
    return when (this) {
        ParticleDeviceType.ARGON -> R.string.product_name_argon
        ParticleDeviceType.BORON -> R.string.product_name_boron
        ParticleDeviceType.XENON -> R.string.product_name_xenon
        else -> throw IllegalArgumentException("$this is not a mesh device type")
    }
}
