package io.particle.android.sdk.ui.devicelist

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.particle.android.common.easyDiffUtilCallback
import io.particle.android.sdk.accountsetup.LoginActivity
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ARGON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.A_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.BLUZ
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.BORON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.B_SOM
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.CORE
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.DIGISTUMP_OAK
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ELECTRON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.OTHER
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.P1
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.PHOTON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.RASPBERRY_PI
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.RED_BEAR_DUO
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.XENON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.X_SOM
import io.particle.android.sdk.cloud.ParticleEvent
import io.particle.android.sdk.cloud.ParticleEventHandler
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary.DeviceSetupCompleteReceiver
import io.particle.android.sdk.ui.InspectorActivity
import io.particle.android.sdk.utils.Py.list
import io.particle.android.sdk.utils.Py.truthy
import io.particle.android.sdk.utils.TLog
import io.particle.android.sdk.utils.ui.Toaster
import io.particle.android.sdk.utils.ui.Ui
import io.particle.commonui.productName
import io.particle.mesh.ui.inflateRow
import io.particle.mesh.ui.setup.MeshSetupActivity
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_device_list2.*
import kotlinx.android.synthetic.main.row_device_list.view.*
import pl.brightinventions.slf4android.LogRecord
import pl.brightinventions.slf4android.NotifyDeveloperDialogDisplayActivity
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.Objects.requireNonNull
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.Level


//FIXME enabling & disabling system events on each refresh as it collides with fetching devices in parallel
class DeviceListFragment : Fragment() {

    companion object {
        fun newInstance() = DeviceListFragment()
    }

    private lateinit var adapter: DeviceListAdapter
    private val filterViewModel: DeviceFilterViewModel by activityViewModels()

    // FIXME: naming, document better
    private var partialContentBar: ProgressBar? = null

    private val subscribeIds = ConcurrentLinkedQueue<Long>()
    private var deviceSetupCompleteReceiver: DeviceSetupCompleteReceiver? = null

    private fun addGen3() {
        addXenonDevice()
        add_device_fab.collapse()
    }

    fun addPhoton() {
        addPhotonDevice()
        add_device_fab.collapse()
    }

    fun addElectron() {
        addElectronDevice()
        add_device_fab.collapse()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val top = inflater.inflate(R.layout.fragment_device_list2, container, false)

        val rv = Ui.findView<RecyclerView>(top, R.id.device_list)
        rv.setHasFixedSize(true)  // perf. optimization
        val layoutManager = LinearLayoutManager(inflater.context)
        rv.layoutManager = layoutManager
        rv.addItemDecoration(
            DividerItemDecoration(requireNonNull<Context>(context), LinearLayout.VERTICAL)
        )

        partialContentBar =
            inflater.inflate(R.layout.device_list_footer, top as ViewGroup, false) as ProgressBar
        partialContentBar!!.visibility = View.INVISIBLE
        partialContentBar!!.layoutParams =
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        adapter = DeviceListAdapter()
        rv.adapter = adapter
        ItemClickSupport.addTo(rv).setOnItemClickListener(
            object : ItemClickSupport.OnItemClickListener {
                override fun onItemClicked(recyclerView: RecyclerView, position: Int, v: View) {
                    onDeviceRowClicked(position)
                }
            }
        )

        return top
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refresh_layout.setOnRefreshListener { this.refreshDevices() }

        deviceSetupCompleteReceiver =
            object : ParticleDeviceSetupLibrary.DeviceSetupCompleteReceiver() {
                override fun onSetupSuccess(id: String) {
                    log.d("Successfully set up $id")
                }

                override fun onSetupFailure() {
                    log.w("Device not set up.")
                }
            }
        deviceSetupCompleteReceiver!!.register(activity)

        refresh_layout.isRefreshing = true

        action_set_up_a_xenon.setOnClickListener { addGen3() }
        action_set_up_a_photon.setOnClickListener { addPhoton() }
        action_set_up_an_electron.setOnClickListener { addElectron() }

        toolbar.inflateMenu(R.menu.device_list)
        toolbar.setOnMenuItemClickListener {
            return@setOnMenuItemClickListener when (it.itemId) {

                R.id.action_log_out -> {
                    AlertDialog.Builder(requireActivity())
                        .setMessage(R.string.logout_confirm_message)
                        .setPositiveButton(R.string.log_out) { dialog, _ ->
                            val cloud = ParticleCloudSDK.getCloud()
                            cloud.logOut()
                            startActivity(Intent(requireContext(), LoginActivity::class.java))
                            requireActivity().finish()
                            dialog.dismiss()
                        }
                        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                        .show()
                    true
                }

                R.id.action_send_logs -> {
                    sendLogs()
                    true
                }

                else -> false
            }
        }

        filter_button.setOnClickListener {
            // TODO: replace this with navigation lib calls
            requireActivity().supportFragmentManager.commit {
                replace(R.id.fragment_parent, DeviceFilterFragment.newInstance())
                addToBackStack(null)
            }
        }

        name_filter_input.afterTextChanged {
            val asStr = it?.toString()

            if (asStr.isNullOrEmpty()) {
                empty_message.setText(R.string.device_list_default_empty_message)
            } else {
                val msg = "No devices found matching '$asStr'"
                empty_message.text = msg
            }

            filterViewModel.updateNameQuery(asStr)
        }

        filterViewModel.filteredDeviceListLD.observe(
            viewLifecycleOwner,
            Observer { onDeviceListUpdated(it) }
        )
    }

    override fun onResume() {
        super.onResume()
        val devices = filterViewModel.fullDeviceListLD.value
//        subscribeToSystemEvents(devices, false)
    }

    override fun onPause() {
        val devices = filterViewModel.fullDeviceListLD.value
//        subscribeToSystemEvents(devices, true)
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        deviceSetupCompleteReceiver!!.unregister(activity)
    }

    private fun onDeviceListUpdated(devices: List<ParticleDevice>?) {
        refresh_layout.isRefreshing = false
        empty_message.isVisible = devices.isNullOrEmpty()
        adapter.submitList(devices)
        //subscribe to system updates
//        subscribeToSystemEvents(devices, false)
    }

    private fun subscribeToSystemEvents(
        devices: List<ParticleDevice>,
        revertSubscription: Boolean
    ) {
        for (device in devices) {
            object : AsyncTask<ParticleDevice, Void, Void>() {
                override fun doInBackground(vararg particleDevices: ParticleDevice): Void? {
                    try {
                        if (revertSubscription) {
                            for (id in subscribeIds) {
                                device.unsubscribeFromEvents(id!!)
                            }
                        } else {
                            subscribeIds.add(
                                device.subscribeToEvents(
                                    "spark/status",
                                    object : ParticleEventHandler {
                                        override fun onEventError(e: Exception) {
                                            //ignore for now, events aren't vital
                                        }

                                        override fun onEvent(
                                            eventName: String,
                                            particleEvent: ParticleEvent
                                        ) {
                                            refreshDevices()
                                        }
                                    })
                            )
                        }
                    } catch (ignore: IOException) {
                        //ignore for now, events aren't vital
                    } catch (ignore: ParticleCloudException) {
                    }

                    return null
                }
            }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, device)
        }
    }

    private fun onDeviceRowClicked(position: Int) {
        log.i("Clicked on item at position: #$position")
        if (position >= adapter.itemCount || position == -1) {
            // we're at the header or footer view, do nothing.
            return
        }

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        val device = filterViewModel.fullDeviceListLD.value!![position]

        if (device.isFlashing) {
            Toaster.s(
                activity,
                "Device is being flashed, please wait for the flashing process to end first"
            )
        } else {
            activity?.let { startActivity(InspectorActivity.buildIntent(it, device)) }
        }
    }

    fun onBackPressed(): Boolean {
        return if (add_device_fab.isExpanded) {
            add_device_fab.collapse()
            true
        } else {
            false
        }
    }

    private fun addXenonDevice() {
        startActivity(Intent(activity, MeshSetupActivity::class.java))
    }

    private fun addPhotonDevice() {
        ParticleDeviceSetupLibrary.startDeviceSetup(
            requireNonNull<FragmentActivity>(activity),
            DeviceListActivity::class.java
        )
    }

    private fun addElectronDevice() {
        //        Intent intent = (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP)
        //                ? new Intent(getActivity(), ElectronSetupActivity.class)
        //                : new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.electron_setup_uri)));
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.electron_setup_uri)))
        startActivity(intent)
    }

    private fun refreshDevices() {
        val devices = filterViewModel.fullDeviceListLD.value
//        subscribeToSystemEvents(devices, true)
        filterViewModel.refreshDevices()

    }

    private fun sendLogs() {
        val lr = LogRecord(Level.WARNING, "")

        NotifyDeveloperDialogDisplayActivity.showDialogIn(
            requireActivity(),
            lr,
            list(),
            "Logs from the Particle Android app",
            "",
            list("pl.brightinventions.slf4android.ReadLogcatEntriesAsyncTask")
        )
    }

}


internal class DeviceListViewHolder(val topLevel: View) : RecyclerView.ViewHolder(topLevel) {
    val modelName: TextView = topLevel.product_model_name
    val deviceName: TextView = topLevel.product_name
    val lastHandshake: TextView = topLevel.last_handshake_text
    val statusDot: ImageView = topLevel.online_status_dot
}


internal class DeviceListAdapter : ListAdapter<ParticleDevice, DeviceListViewHolder>(
    easyDiffUtilCallback { device: ParticleDevice -> device.id }
) {

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy, HH:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceListViewHolder {
        return DeviceListViewHolder(parent.inflateRow(R.layout.row_device_list))
    }

    override fun onBindViewHolder(holder: DeviceListViewHolder, position: Int) {
        val device = getItem(position)

        val ctx = holder.topLevel.context

        holder.modelName.setText(device.deviceType!!.productName)
        holder.lastHandshake.text = device.lastHeard?.let { dateFormatter.format(it) }
        holder.statusDot.setImageDrawable(ctx.getDrawable(getStatusDotRes(device)))

        @ColorInt val colorValue: Int = ContextCompat.getColor(
            ctx,
            device.deviceType!!.getColorForDeviceType()
        )
        val bg = holder.modelName.background
        bg.mutate()
        DrawableCompat.setTint(bg, colorValue)
        holder.modelName.setTextColor(colorValue)

        val name = if (truthy(device.name))
            device.name
        else
            ctx.getString(R.string.unnamed_device)
        holder.deviceName.text = name
    }

    private fun getStatusDotRes(device: ParticleDevice): Int {
        return when {
            device.isFlashing -> R.drawable.device_flashing_dot
            device.isConnected -> R.drawable.online_dot
            else -> R.drawable.offline_dot
        }
    }
}


private val log = TLog.get(DeviceListFragment::class.java)


@ColorRes
private fun ParticleDeviceType.getColorForDeviceType(): Int {
    return when (this) {
        CORE -> R.color.spark_blue
        ELECTRON -> R.color.device_color_electron
        PHOTON,
        P1 -> R.color.device_color_photon
        RASPBERRY_PI -> R.color.wisteria
        RED_BEAR_DUO -> R.color.orange
//        ESP32 -> 0x000000
        BLUZ -> R.color.belize
        ARGON, BORON, XENON,
        A_SOM, B_SOM, X_SOM -> R.color.emerald
        DIGISTUMP_OAK,
        OTHER -> R.color.gray
    }
}


private fun ParticleDeviceType.getIconText(): String {
    return when (this) {
        CORE -> "C"
        ELECTRON -> "E"
        PHOTON -> "P"
        P1 -> "1"
        RASPBERRY_PI -> "R"
        RED_BEAR_DUO -> "D"
//        ESP32 -> "ES"
        BLUZ -> "BZ"
        ARGON, A_SOM -> "A"
        BORON, B_SOM -> "B"
        XENON, X_SOM -> "X"
        else -> "?"
    }
}
