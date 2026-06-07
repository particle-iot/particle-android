package io.particle.android.sdk.ui.devicelist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.text.HtmlCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.leinardi.android.speeddial.SpeedDialActionItem
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
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary.DeviceSetupCompleteReceiver
import io.particle.android.sdk.ui.InspectorActivity
import io.particle.android.sdk.utils.Py.truthy
import io.particle.android.sdk.utils.TLog
import io.particle.android.sdk.utils.ui.Toaster
import io.particle.android.sdk.utils.ui.Ui
import io.particle.commonui.BreathingGlow
import io.particle.commonui.styleAsPill
import io.particle.mesh.common.android.livedata.nonNull
import io.particle.mesh.common.android.livedata.runBlockOnUiThreadAndAwaitUpdate
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.ui.inflateRow
import io.particle.mesh.ui.setup.MeshSetupActivity
import io.particle.sdk.app.R
import io.particle.sdk.app.databinding.FragmentDeviceList2Binding
import io.particle.sdk.app.databinding.RowDeviceListBinding
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

    private var _binding: FragmentDeviceList2Binding? = null
    private val binding get() = _binding!!

    private fun addGen3() {
        addXenonDevice()
        binding.addDeviceFab.close()
    }

    fun addPhoton() {
        addPhotonDevice()
        binding.addDeviceFab.close()
    }

    fun addElectron() {
        addElectronDevice()
        binding.addDeviceFab.close()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentDeviceList2Binding.inflate(inflater, container, false)
        val top = binding.root

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nameFilterTextWatcher = buildNameFilterTextWatcher()

        binding.refreshLayout.setOnRefreshListener { this.refreshDevices() }

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

        binding.refreshLayout.isRefreshing = true

        val fabIconTint = ContextCompat.getColor(requireContext(), R.color.accent_color)
        val fabBackground = ContextCompat.getColor(requireContext(), R.color.white)
        binding.addDeviceFab.addAllActionItems(
            listOf(
                SpeedDialActionItem.Builder(R.id.action_set_up_a_xenon, R.drawable.ic_add_white_24dp)
                    .setLabel("Setup a Gen 3 device")
                    .setFabBackgroundColor(fabBackground)
                    .setFabImageTintColor(fabIconTint)
                    .create(),
                SpeedDialActionItem.Builder(R.id.action_set_up_a_photon, R.drawable.ic_add_white_24dp)
                    .setLabel("Setup a Photon")
                    .setFabBackgroundColor(fabBackground)
                    .setFabImageTintColor(fabIconTint)
                    .create(),
                SpeedDialActionItem.Builder(R.id.action_set_up_an_electron, R.drawable.ic_add_white_24dp)
                    .setLabel("Setup an Electron")
                    .setFabBackgroundColor(fabBackground)
                    .setFabImageTintColor(fabIconTint)
                    .create()
            )
        )
        binding.addDeviceFab.setOnActionSelectedListener { actionItem ->
            when (actionItem.id) {
                R.id.action_set_up_a_xenon -> addGen3()
                R.id.action_set_up_a_photon -> addPhoton()
                R.id.action_set_up_an_electron -> addElectron()
            }
            // returning false closes the speed-dial menu after the action runs
            false
        }

        setUpNavigationDrawer()

        binding.filterButton.setOnClickListener {
            // TODO: replace this with navigation lib calls
            requireActivity().supportFragmentManager.commit {
                replace(R.id.fragment_parent, DeviceFilterFragment.newInstance())
                addToBackStack(null)
            }
        }

        binding.searchIcon.setOnClickListener {
            binding.nameFilterInput.requestFocus()
            val imm: InputMethodManager? = requireContext().getSystemService()
            imm?.showSoftInput(binding.nameFilterInput, InputMethodManager.SHOW_IMPLICIT)
        }
        binding.clearTextIcon.setOnClickListener { binding.nameFilterInput.setText("") }
    }

    override fun onResume() {
        super.onResume()
        binding.nameFilterInput.addTextChangedListener(nameFilterTextWatcher)
        subscribeToSystemEvents()
        filterViewModel.currentDeviceFilter.filteredDeviceListLD.nonNull().observe(
            viewLifecycleOwner,
            Observer { onDeviceListUpdated(it) }
        )
    }

    override fun onPause() {
        filterViewModel.currentDeviceFilter.filteredDeviceListLD.removeObservers(viewLifecycleOwner)
        binding.nameFilterInput.removeTextChangedListener(nameFilterTextWatcher)
        unsubscribeFromSystemEvents()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        deviceSetupCompleteReceiver?.unregister(activity)
    }

    private fun onDeviceListUpdated(devices: List<ParticleDevice>) {
        log.i("onDeviceListUpdated(): $devices")
        val ctx = context ?: return

        binding.refreshLayout.isRefreshing = false

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
        binding.filterButton.setImageDrawable(filterIcon)
        binding.filterButton.background = filterBg

        updateEmptyMessageAndSearchBox()

        binding.emptyMessage.isVisible = devices.isNullOrEmpty()
        adapter.submitList(devices)
        adapter.notifyDataSetChanged()
    }

    private fun updateEmptyMessageAndSearchBox() {
        val config = filterViewModel.currentDeviceFilter.deviceListViewConfigLD.value!!
        if (filterViewModel.fullDeviceListLD.value.isNullOrEmpty()) {
            binding.emptyMessage.setText(R.string.device_list_default_empty_message)
        } else {
            if (config.deviceNameQueryString.isNullOrBlank()) {
                binding.emptyMessage.text = "No devices found matching the current filter"
            } else {
                val msg = "No devices found matching '${config.deviceNameQueryString}'"
                binding.emptyMessage.text = msg
            }
        }

        binding.clearTextIcon.isVisible = !config.deviceNameQueryString.isNullOrEmpty()
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
        return when {
            binding.drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                true
            }
            binding.addDeviceFab.isOpen -> {
                binding.addDeviceFab.close()
                true
            }
            else -> false
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
        // Electron (cellular) setup moved to the web (setup.particle.io) and needs a USB
        // connection, so it can't be done on-device. Explain where to go rather than opening a
        // mobile browser, and offer to copy the link.
        val setupUrl = getString(R.string.electron_setup_uri)
        val message = HtmlCompat.fromHtml(
            getString(R.string.electron_setup_moved_message),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        AlertDialog.Builder(requireActivity())
            .setTitle(R.string.electron_setup_moved_title)
            .setMessage(message)
            .setPositiveButton(R.string.got_it, null)
            .setNeutralButton(R.string.copy_link) { _, _ ->
                val clipboard = requireContext().getSystemService<ClipboardManager>()
                clipboard?.setPrimaryClip(ClipData.newPlainText("Particle setup", setupUrl))
                Toaster.s(activity, getString(R.string.link_copied))
            }
            .show()
    }

    private fun refreshDevices() {
        filterViewModel.refreshDevices()
    }

    private fun setUpNavigationDrawer() {
        // Hamburger button in the toolbar opens the slide-out drawer.
        binding.toolbar.navigationIcon =
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_menu_white_24dp)
        binding.toolbar.navigationContentDescription = getString(R.string.menu_open_navigation)
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // The drawer dims the content behind it with a ~60% black scrim, but the system status
        // bar above the toolbar isn't covered by that scrim. Darken the status-bar colour in step
        // with the slide so the whole top of the screen dims together.
        val window = requireActivity().window
        val statusBarBase = ContextCompat.getColor(requireContext(), R.color.p_particle_navy)
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                window.statusBarColor = ColorUtils.blendARGB(statusBarBase, Color.BLACK, 0.6f * slideOffset)
            }

            override fun onDrawerClosed(drawerView: View) {
                window.statusBarColor = statusBarBase
            }
        })

        val drawer = binding.drawer
        drawer.drawerEmail.text = ParticleCloudSDK.getCloud().loggedInUsername ?: ""

        val version = try {
            val pInfo = requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0)
            // The build number is the trailing component of our EPOCH.MAJOR.MINOR.PATCH.BUILD
            // versionCode encoding (e.g. 1_04_00_00_01 -> build 1).
            val build = PackageInfoCompat.getLongVersionCode(pInfo) % 100
            "Tinker ${pInfo.versionName} ($build)"
        } catch (ex: Exception) {
            "Tinker"
        }
        drawer.drawerVersion.text = version

        drawer.drawerDocs.setOnClickListener { openUrlFromDrawer("https://docs.particle.io") }
        drawer.drawerConsole.setOnClickListener { openUrlFromDrawer("https://console.particle.io") }
        drawer.drawerPrivacy.setOnClickListener {
            openUrlFromDrawer("https://www.particle.io/legal/privacy/")
        }
        drawer.drawerLogout.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            confirmAndLogOut()
        }
    }

    private fun openUrlFromDrawer(url: String) {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (ex: Exception) {
            Toaster.s(activity, "No app available to open this link")
        }
    }

    private fun confirmAndLogOut() {
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
    }
}


internal class DeviceListViewHolder(val topLevel: View) : RecyclerView.ViewHolder(topLevel) {
    private val binding = RowDeviceListBinding.bind(topLevel)
    val modelName: TextView = binding.productModelName
    val deviceName: TextView = binding.productName
    val lastHandshake: TextView = binding.lastHandshakeText
    val statusDot: ImageView = binding.onlineStatusDot
    val statusGlow = BreathingGlow(statusDot)
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

    override fun onViewRecycled(holder: DeviceListViewHolder) {
        // Stop the glow ticker so recycled rows don't keep posting callbacks.
        holder.statusGlow.stop()
    }

    override fun onBindViewHolder(holder: DeviceListViewHolder, position: Int) {
        val device = getItem(position)

        val ctx = holder.topLevel.context

        holder.modelName.styleAsPill(device.deviceType!!)
        holder.lastHandshake.text = device.lastHeard?.let { dateFormatter.format(it) }
        holder.statusDot.setImageDrawable(ctx.getDrawable(getStatusDotRes(device)))
        // "Breathing" glow for online devices, throttled to ~15fps (see BreathingGlow).
        if (device.isConnected) holder.statusGlow.start() else holder.statusGlow.stop()

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
