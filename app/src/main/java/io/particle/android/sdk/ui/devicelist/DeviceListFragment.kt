package io.particle.android.sdk.ui.devicelist

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
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
import com.snakydesign.livedataextensions.nonNull
import io.particle.android.common.easyDiffUtilCallback
import io.particle.android.sdk.accountsetup.LoginActivity
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.cloud.ParticleEvent
import io.particle.android.sdk.cloud.ParticleEventHandler
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary.DeviceSetupCompleteReceiver
import io.particle.android.sdk.ui.InspectorActivity
import io.particle.commonui.toDecorationColor
import io.particle.android.sdk.utils.Py.list
import io.particle.android.sdk.utils.Py.truthy
import io.particle.android.sdk.utils.TLog
import io.particle.android.sdk.utils.ui.Toaster
import io.particle.android.sdk.utils.ui.Ui
import io.particle.commonui.productName
import io.particle.commonui.styleAsPill
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.setup.flow.Scopes
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

    private lateinit var nameFilterTextWatcher: TextWatcher
    private val subscribeIds = ConcurrentLinkedQueue<Long>()
    private var deviceSetupCompleteReceiver: DeviceSetupCompleteReceiver? = null

    private val scopes = Scopes()

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

        adapter = DeviceListAdapter { onDeviceRowClicked(it) }
        rv.adapter = adapter

        return top
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nameFilterTextWatcher = buildNameFilterTextWatcher()

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


        filterViewModel.currentDeviceFilter.filteredDeviceListLD.nonNull().observe(
            viewLifecycleOwner,
            Observer { onDeviceListUpdated(it) }
        )
    }

    override fun onResume() {
        super.onResume()
        name_filter_input.addTextChangedListener(nameFilterTextWatcher)
        val devices = filterViewModel.fullDeviceListLD.value
//        subscribeToSystemEvents(devices, false)

    }

    override fun onPause() {
        name_filter_input.removeTextChangedListener(nameFilterTextWatcher)
        val devices = filterViewModel.fullDeviceListLD.value
//        subscribeToSystemEvents(devices, true)
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        deviceSetupCompleteReceiver!!.unregister(activity)
    }

    private fun onDeviceListUpdated(devices: List<ParticleDevice>) {
        val ctx = context ?: return

        refresh_layout.isRefreshing = false

        val currentConfig = filterViewModel.currentDeviceFilter.deviceListViewConfigLD.value
        val (filterIcon, filterBg) = if (currentConfig == defaultDeviceListConfig) {
            Pair(
                ctx.getDrawable(R.drawable.ic_filter_list_gray_24dp),
                null
            )
        } else {
            val gray = ctx.getDrawable(R.drawable.ic_filter_list_gray_24dp)!!
            gray.mutate()
            val white = ContextCompat.getColor(ctx, android.R.color.white)
            DrawableCompat.setTint(gray, white)
            Pair(gray, ctx.getDrawable(R.drawable.bg_device_filter_active))
        }
        filter_button.setImageDrawable(filterIcon)
        filter_button.background = filterBg

        updateEmptyMessageAndSearchBox()

        empty_message.isVisible = devices.isNullOrEmpty()
        adapter.submitList(devices)
        //subscribe to system updates
//        subscribeToSystemEvents(devices, false)
    }

    private fun updateEmptyMessageAndSearchBox() {
        val config = filterViewModel.currentDeviceFilter.deviceListViewConfigLD.value!!
        if (filterViewModel.fullDeviceListLD.value.isNullOrEmpty()) {
            empty_message.setText(R.string.device_list_default_empty_message)
        } else {
            if (config.deviceNameQueryString.isNullOrBlank()) {
                empty_message.text = "No devices found matching the current filter"
            } else {
                val msg = "No devices found matching '${config.deviceNameQueryString}'"
                empty_message.text = msg
            }
        }

        val filteredDevices = filterViewModel.currentDeviceFilter.filteredDeviceListLD.value!!
        val completeDevices = filterViewModel.fullDeviceListLD.value!!
        if (completeDevices.size == filteredDevices.size) {
            name_filter_input.hint = "Search devices"
        } else {
            name_filter_input.hint = "Search in ${filteredDevices.size} of ${completeDevices.size} devices"
        }
    }

    private fun buildNameFilterTextWatcher(): TextWatcher {
        return afterTextChangedListener {
            val asStr = it?.toString()

            scopes.onMain {
                filterViewModel.draftDeviceFilter.deviceListViewConfigLD
                    .nonNull(scopes)
                    .runBlockOnUiThreadAndAwaitUpdate(scopes) {
                        filterViewModel.draftDeviceFilter.updateNameQuery(asStr)
                    }
                filterViewModel.draftDeviceFilter.commitDraftConfig()
            }
        }
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

    private fun onDeviceRowClicked(device: ParticleDevice) {
        log.i("Clicked on device=$device")
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


internal class DeviceListAdapter(
    private val onClickHandler: (ParticleDevice) -> Unit
) : ListAdapter<ParticleDevice, DeviceListViewHolder>(
    easyDiffUtilCallback { device: ParticleDevice -> device.id }
) {

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy, HH:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceListViewHolder {
        return DeviceListViewHolder(parent.inflateRow(R.layout.row_device_list))
    }

    override fun onBindViewHolder(holder: DeviceListViewHolder, position: Int) {
        val device = getItem(position)

        val ctx = holder.topLevel.context

        holder.modelName.styleAsPill(device.deviceType!!)
        holder.lastHandshake.text = device.lastHeard?.let { dateFormatter.format(it) }
        holder.statusDot.setImageDrawable(ctx.getDrawable(getStatusDotRes(device)))
        holder.statusDot.animation?.cancel()
        if (device.isConnected) {
            val animFade = AnimationUtils.loadAnimation(ctx, R.anim.fade_in_out)
            holder.statusDot.startAnimation(animFade)
        }

        val name = if (truthy(device.name))
            device.name
        else
            ctx.getString(R.string.unnamed_device)
        holder.deviceName.text = name

        holder.topLevel.setOnClickListener { onClickHandler(device) }
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
