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
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ARGON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.A_SERIES
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.BORON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.B_SERIES
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.XENON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.X_SERIES
import io.particle.mesh.R
import io.particle.mesh.setup.flow.Gen3ConnectivityType
import io.particle.mesh.setup.toConnectivityType
import io.particle.mesh.setup.ui.utils.easyDiffUtilCallback
import io.particle.mesh.setup.ui.utils.inflateRow
import io.particle.mesh.setup.ui.utils.localDeviceHasInternetConnection
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
//                    Gen3ConnectivityType.MESH_ONLY,
//                    R.string.product_description_xenon,
//                    R.drawable.xenon_vector
//                ),
//                DeviceData(
//                    Gen3ConnectivityType.WIFI,
//                    R.string.product_description_argon,
//                    R.drawable.argon_vector
//                ),
//                DeviceData(
//                    Gen3ConnectivityType.CELLULAR,
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

    private fun onItemClicked(@Suppress("UNUSED_PARAMETER") deviceData: DeviceData) {
        // FIXME: this should happen via FlowManager
        // check for internet access
//        if (!requireActivity().localDeviceHasInternetConnection()) {
//            showNoInternetDialog()
//            return
//        }
//
//        FlowManagerAccessModel.getViewModel(this).startFlowForDevice(deviceData.connectivityType)
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
    val connectivityType: Gen3ConnectivityType = deviceType.toConnectivityType()
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
    easyDiffUtilCallback { deviceData: DeviceData -> deviceData.connectivityType }
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

        when (item.connectivityType) {
            Gen3ConnectivityType.WIFI -> {
                holder.capability2.setImageResource(R.drawable.p_mesh_ic_capability_wifi)
            }
            Gen3ConnectivityType.CELLULAR -> {
                holder.capability2.setImageResource(R.drawable.p_mesh_ic_capability_cellular)
            }
            Gen3ConnectivityType.MESH_ONLY -> { /* no-op */ }
        }
    }
}


@StringRes
private fun ParticleDeviceType.toDisplayName(): Int {
    return when (this) {
        ARGON -> R.string.product_name_argon
        BORON -> R.string.product_name_boron
        XENON -> R.string.product_name_xenon
        A_SERIES -> R.string.product_name_a_series
        B_SERIES -> R.string.product_name_b_series
        X_SERIES -> R.string.product_name_x_series
        else -> throw IllegalArgumentException("Not a mesh device: $this")
    }
}
