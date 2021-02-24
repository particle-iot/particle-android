package io.particle.android.sdk.ui.devicelist

import android.app.Dialog
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
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.snakydesign.livedataextensions.nonNull
import io.particle.android.common.easyDiffUtilCallback
import io.particle.android.sdk.accountsetup.LoginActivity
import io.particle.android.sdk.cloud.*
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary.DeviceSetupCompleteReceiver
import io.particle.android.sdk.ui.InspectorActivity
import io.particle.android.sdk.ui.devicelist.UserServiceAgreementsCheckResult.LimitReached
import io.particle.android.sdk.ui.devicelist.UserServiceAgreementsCheckResult.NetworkError
import io.particle.android.sdk.ui.devicelist.UserServiceAgreementsCheckResult.SetupAllowed
import io.particle.android.sdk.utils.Py.truthy
import io.particle.android.sdk.utils.TLog
import io.particle.android.sdk.utils.ui.Toaster
import io.particle.android.sdk.utils.ui.Ui
import io.particle.commonui.styleAsPill
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.utils.safeToast
import io.particle.mesh.ui.inflateRow
import io.particle.mesh.ui.setup.MeshSetupActivity
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_device_list2.*
import kotlinx.android.synthetic.main.row_device_list.view.*
import pl.brightinventions.slf4android.LogTask
import pl.brightinventions.slf4android.showLogSharingPrompt
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.Objects.requireNonNull
import java.util.concurrent.ConcurrentLinkedQueue


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
    private var dialog: Dialog? = null
    private var shouldRefreshOnResume = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

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

        refresh_layout.setOnRefreshListener { this.refreshData() }

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

        action_set_up_a_xenon.setOnClickListener { onSetupButtonPressed(SetupInitType.GEN3) }
        action_set_up_a_photon.setOnClickListener { onSetupButtonPressed(SetupInitType.PHOTON) }
        action_set_up_an_electron.setOnClickListener { onSetupButtonPressed(SetupInitType.ELECTRON) }

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

        search_icon.setOnClickListener {
            name_filter_input.requestFocus()
            val imm: InputMethodManager? = requireContext().getSystemService()
            imm?.showSoftInput(name_filter_input, InputMethodManager.SHOW_IMPLICIT)
        }
        clear_text_icon.setOnClickListener { name_filter_input.setText("") }
    }

    override fun onResume() {
        super.onResume()
        name_filter_input.addTextChangedListener(nameFilterTextWatcher)
        subscribeToSystemEvents()
        filterViewModel.currentDeviceFilter.filteredDeviceListLD.nonNull().observe(
            viewLifecycleOwner,
            { onDeviceListUpdated(it) }
        )
        if (shouldRefreshOnResume) {
            shouldRefreshOnResume = false
            filterViewModel.refreshDevices()
        }
    }

    override fun onPause() {
        filterViewModel.currentDeviceFilter.filteredDeviceListLD.removeObservers(viewLifecycleOwner)
        name_filter_input.removeTextChangedListener(nameFilterTextWatcher)
        unsubscribeFromSystemEvents()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        scopes.cancelChildren()
        dialog?.cancel()
        dialog = null
    }

    override fun onDestroy() {
        super.onDestroy()
        deviceSetupCompleteReceiver?.unregister(activity)
    }

    private fun onDeviceListUpdated(devices: List<ParticleDevice>) {
        log.i("onDeviceListUpdated(): $devices")
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
        adapter.notifyDataSetChanged()
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

        clear_text_icon.isVisible = !config.deviceNameQueryString.isNullOrEmpty()
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

    private fun unsubscribeFromSystemEvents() {
        scopes.onWorker {
            val ids = subscribeIds.toLongArray()
            subscribeIds.clear()
            for (id in ids) {
                ParticleCloudSDK.getCloud().unsubscribeFromEventWithID(id)
            }
        }
    }

    private fun subscribeToSystemEvents() {
        scopes.onWorker {
            val cloud = ParticleCloudSDK.getCloud()
            val eventList = listOf(
                "spark/status",
                "spark/flash/status",
                "spark/device/app-hash",
                "spark/status/safe-mode",
                "spark/safe-mode-updater/updating"
            )
            for (event in eventList) {
                try {
                    val subscriberId = cloud.subscribeToMyDevicesEvents(
                        event,
                        object: ParticleEventHandler {
                            override fun onEventError(e: java.lang.Exception?) {
                                // ignore
                            }

                            override fun onEvent(eventName: String?, particleEvent: ParticleEvent?) {
                                scopes.onWorker {
                                    particleEvent?.deviceId?.let {
                                        try {
                                            val device = cloud.getDevice(it)
                                            log.i("Refreshing device from event $eventName")
                                            device.refresh()
                                        } catch (ex: Exception) {
                                            // ignore
                                        }
                                    }
                                }
                            }
                        }
                    )
                    subscribeIds.add(subscriberId)
                } catch (ex: Exception) {
                    // ignore
                }
            }

            log.i("Subscriber IDs: $subscribeIds")
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

    private fun onSetupButtonPressed(initType: SetupInitType) {
        add_device_fab.collapse()
        dialog = showProgressDialog()
        scopes.onMain {
            val checkResult = filterViewModel.checkServiceAgreements()

            if (dialog?.isShowing != true) {
                // dialog was already closed by the user/by leaving this screen
                return@onMain
            }

            dialog?.cancel()
            dialog = null

            when (checkResult) {
                is SetupAllowed -> continueWithSetup(initType)
                is LimitReached -> showLimitReachedDialog(checkResult.maxDevices)
                is NetworkError -> activity.safeToast(
                    getString(R.string.MeshStrings_Error_NetworkError)
                )
            }
        }
    }

    private fun continueWithSetup(initType: SetupInitType) {
        shouldRefreshOnResume = true

        when (initType) {

            SetupInitType.GEN3 -> startActivity(Intent(activity, MeshSetupActivity::class.java))

            SetupInitType.PHOTON -> {
                ParticleDeviceSetupLibrary.startDeviceSetup(
                    requireNonNull<FragmentActivity>(activity),
                    DeviceListActivity::class.java
                )
            }

            SetupInitType.ELECTRON -> {
                val uri = Uri.parse(getString(R.string.electron_setup_uri))
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
        }
    }

    private fun refreshData() {
        filterViewModel.refreshDevices()
    }

    private fun sendLogs() {
        showLogSharingPrompt(
            requireActivity(),
            "",
            listOf(),
            "Logs from the Particle Android app",
            "",
            mutableListOf<AsyncTask<Context, Void, File>>(LogTask())
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


private enum class SetupInitType {
    GEN3,
    PHOTON,
    ELECTRON
}



private val log = TLog.get(DeviceListFragment::class.java)
