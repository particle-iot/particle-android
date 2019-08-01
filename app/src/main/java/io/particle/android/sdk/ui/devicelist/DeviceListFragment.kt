package io.particle.android.sdk.ui.devicelist

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.Callback
import io.particle.android.sdk.DevicesLoader
import io.particle.android.sdk.DevicesLoader.DevicesLoadResult
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.cloud.ParticleEvent
import io.particle.android.sdk.cloud.ParticleEventHandler
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary.DeviceSetupCompleteReceiver
import io.particle.android.sdk.ui.InspectorActivity
import io.particle.android.sdk.ui.devicelist.Comparators.BooleanComparator
import io.particle.android.sdk.ui.devicelist.Comparators.ComparatorChain
import io.particle.android.sdk.ui.devicelist.Comparators.NullComparator
import io.particle.android.sdk.utils.EZ
import io.particle.android.sdk.utils.Py.list
import io.particle.android.sdk.utils.Py.truthy
import io.particle.android.sdk.utils.TLog
import io.particle.android.sdk.utils.ui.Fragments
import io.particle.android.sdk.utils.ui.Toaster
import io.particle.android.sdk.utils.ui.Ui
import io.particle.commonui.productImage
import io.particle.commonui.productName
import io.particle.mesh.ui.setup.MeshSetupActivity
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_device_list2.*
import kotlinx.android.synthetic.main.row_device_list.view.*
import java.io.IOException
import java.util.*
import java.util.Objects.requireNonNull
import java.util.concurrent.ConcurrentLinkedQueue


//FIXME enabling & disabling system events on each refresh as it collides with fetching devices in parallel
class DeviceListFragment : Fragment(), LoaderManager.LoaderCallbacks<DevicesLoadResult> {

    // A no-op impl of {@link Callbacks}. Used when this fragment is not attached to an activity.
    private val dummyCallbacks = object : Callbacks {
        override fun onDeviceSelected(device: ParticleDevice) {}
    }

    private lateinit var adapter: DeviceListAdapter
    // FIXME: naming, document better
    private var partialContentBar: ProgressBar? = null
    private var isLoadingSnackbarVisible: Boolean = false

    private val subscribeIds = ConcurrentLinkedQueue<Long>()
    private val reloadStateDelegate = ReloadStateDelegate()
    private val comparator = helpfulOrderDeviceComparator()

    private var callbacks: Callbacks = dummyCallbacks
    private var deviceSetupCompleteReceiver: DeviceSetupCompleteReceiver? = null

    val textFilter: String?
        get() = adapter.textFilter

    internal interface Callbacks {
        fun onDeviceSelected(device: ParticleDevice)
    }

    fun addXenon() {
        addXenonDevice()
        add_device_fab.collapse()
    }

    fun addPhoton() {
        addPhotonDevice()
        add_device_fab.collapse()
    }

    fun addCore() {
        addSparkCoreDevice()
        add_device_fab.collapse()
    }

    fun addElectron() {
        addElectronDevice()
        add_device_fab.collapse()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        callbacks = Fragments.getCallbacksOrThrow<Callbacks>(this, Callbacks::class.java)
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
            DividerItemDecoration(
                requireNonNull<Context>(context),
                LinearLayout.VERTICAL
            )
        )

        partialContentBar =
            inflater.inflate(R.layout.device_list_footer, top as ViewGroup, false) as ProgressBar
        partialContentBar!!.visibility = View.INVISIBLE
        partialContentBar!!.layoutParams =
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        adapter = DeviceListAdapter(requireNonNull<FragmentActivity>(activity))
        rv.adapter = adapter
        ItemClickSupport.addTo(rv).setOnItemClickListener { _, position, _ ->
            onDeviceRowClicked(position)
        }
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

        LoaderManager.getInstance(this).initLoader(R.id.device_list_devices_loader_id, null, this)
        refresh_layout.isRefreshing = true

        if (savedInstanceState != null) {
            adapter.filter(savedInstanceState.getString("txtFilterState"))
        }

        action_set_up_a_xenon.setOnClickListener { addXenon() }
        action_set_up_a_photon.setOnClickListener { addPhoton() }
        action_set_up_a_core.setOnClickListener { addCore() }
        action_set_up_an_electron.setOnClickListener { addElectron() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("txtFilterState", adapter.textFilter)
    }

    override fun onResume() {
        super.onResume()
        val devices = adapter.items
//        subscribeToSystemEvents(devices, false)
    }

    override fun onStart() {
        super.onStart()
        refreshDevices()
    }

    override fun onPause() {
        val devices = adapter.items
//        subscribeToSystemEvents(devices, true)
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        refresh_layout.isRefreshing = false
        add_device_fab.collapse()
        reloadStateDelegate.reset()
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = dummyCallbacks
    }

    override fun onDestroy() {
        super.onDestroy()
        deviceSetupCompleteReceiver!!.unregister(activity)
    }

    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<DevicesLoadResult> {
        return DevicesLoader(activity)
    }

    override fun onLoadFinished(loader: Loader<DevicesLoadResult>, result: DevicesLoadResult) {
        refresh_layout.isRefreshing = false

        val devices = ArrayList(result.devices)
        Collections.sort(devices, comparator)

        reloadStateDelegate.onDeviceLoadFinished(loader, result)

        adapter.clear()
        adapter.addAll(devices)
        adapter.notifyDataSetChanged()

        empty_message.isVisible = (adapter.itemCount == 0)
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

    override fun onLoaderReset(loader: Loader<DevicesLoadResult>) {
        // no-op
    }

    private fun onDeviceRowClicked(position: Int) {
        log.i("Clicked on item at position: #$position")
        if (position >= adapter.itemCount || position == -1) {
            // we're at the header or footer view, do nothing.
            return
        }

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        val device = adapter.getItem(position)

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
        if (add_device_fab.isExpanded) {
            add_device_fab.collapse()
            return true
        } else {
            return false
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

    private fun addSparkCoreDevice() {
        try {
            val coreAppPkg = "io.spark.core.android"
            // Is the spark core app already installed?
            var intent =
                requireNonNull<FragmentActivity>(activity).packageManager.getLaunchIntentForPackage(
                    coreAppPkg
                )
            if (intent == null) {
                // Nope.  Send the user to the store.
                intent = Intent(Intent.ACTION_VIEW)
                    .setData(Uri.parse("market://details?id=$coreAppPkg"))
            }
            startActivity(intent)
        } catch (ignored: ActivityNotFoundException) {
            Toast.makeText(activity, "Cannot find spark core application.", Toast.LENGTH_SHORT)
                .show()
        }

    }

    private fun addElectronDevice() {
        //        Intent intent = (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP)
        //                ? new Intent(getActivity(), ElectronSetupActivity.class)
        //                : new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.electron_setup_uri)));
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.electron_setup_uri)))
        startActivity(intent)
    }

    private fun refreshDevices() {
        val devices = adapter.items
//        subscribeToSystemEvents(devices, true)
        val loader = loaderManager.getLoader<Any>(R.id.device_list_devices_loader_id)
        loader!!.forceLoad()
    }

    fun filter(typeArrayList: List<ParticleDeviceType?>) {
        adapter.filter(typeArrayList)
        empty_message.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    fun filter(query: String) {
        adapter.filter(query)
        empty_message.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    internal class DeviceListAdapter(private val activity: FragmentActivity) :
        RecyclerView.Adapter<DeviceListAdapter.ViewHolder>() {

        private val devices = list<ParticleDevice>()
        private val filteredData = list<ParticleDevice>()
        private var defaultBackground: Drawable? = null
        var textFilter: String? = ""
            private set
        private var typeFilters: List<ParticleDeviceType?> = Arrays.asList(
            *ParticleDeviceType.values()
        )

        val items: List<ParticleDevice>
            get() = devices

        internal class ViewHolder(val topLevel: View) : RecyclerView.ViewHolder(topLevel) {
            var modelName: TextView = topLevel.product_model_name
            var productImage: AppCompatImageView = topLevel.product_image
            var deviceName: TextView = topLevel.product_name
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // create a new view
            val v = LayoutInflater.from(parent.context).inflate(
                R.layout.row_device_list, parent, false
            )
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val device = filteredData[position]

            if (defaultBackground == null) {
                defaultBackground = holder.topLevel.background
            }
            holder.topLevel.setBackgroundResource(R.color.device_item_bg)

            holder.modelName.setText(device.deviceType!!.productName)
            holder.productImage.setImageResource(device.deviceType!!.productImage)

            val ctx = holder.topLevel.context
            val name = if (truthy(device.name))
                device.name
            else
                ctx.getString(R.string.unnamed_device)
            holder.deviceName.text = name
        }

        override fun getItemCount(): Int {
            return filteredData.size
        }

        fun clear() {
            devices.clear()
            filteredData.clear()
            notifyDataSetChanged()
        }

        fun addAll(toAdd: List<ParticleDevice>) {
            devices.addAll(toAdd)
            filter(textFilter, typeFilters)
        }

        fun filter(query: String?) {
            textFilter = query
            filteredData.clear()
            notifyDataSetChanged()

            filter(query, typeFilters)
        }

        fun filter(typeArrayList: List<ParticleDeviceType?>) {
            typeFilters = typeArrayList
            filteredData.clear()
            notifyDataSetChanged()

            filter(textFilter, typeArrayList)
        }

        private fun filter(query: String?, typeArrayList: List<ParticleDeviceType?>) {
            for (device in devices) {
                if ((containsFilter(device.name, query)
                            || containsFilter(device.deviceType!!.name, query)
                            || containsFilter(device.currentBuild, query)
                            || containsFilter(device.iccid, query)
                            || containsFilter(device.id, query)
                            || containsFilter(
                        device.imei,
                        query
                    )) && typeArrayList.contains(device.deviceType)
                ) {
                    filteredData.add(device)
                    notifyItemInserted(devices.indexOf(device))
                }
            }
        }

        fun getItem(position: Int): ParticleDevice {
            return devices[position]
        }
    }


    private inner class ReloadStateDelegate {

        private val MAX_RETRIES = 10

        internal var retryCount = 0

        internal fun onDeviceLoadFinished(
            loader: Loader<DevicesLoadResult>,
            result: DevicesLoadResult
        ) {
            if (!result.isPartialResult) {
                reset()
                return
            }

            retryCount++
            if (retryCount > MAX_RETRIES) {
                // tried too many times, giving up. :(
                partialContentBar!!.visibility = View.INVISIBLE
                return
            }

            if (!isLoadingSnackbarVisible) {
                isLoadingSnackbarVisible = true
                val view = view
                if (view != null) {
                    Snackbar.make(view, "Unable to load all devices", Snackbar.LENGTH_SHORT)
                        .addCallback(object : Callback() {
                            override fun onDismissed(snackbar: Snackbar?, event: Int) {
                                super.onDismissed(snackbar, event)
                                isLoadingSnackbarVisible = false
                            }
                        }).show()
                }
            }

            partialContentBar!!.visibility = View.VISIBLE
            (loader as DevicesLoader).setUseLongTimeoutsOnNextLoad(true)
            // FIXME: is it READY_TO_ACTIVATE to call forceLoad() in loader callbacks?  Test and be certain.
            EZ.runOnMainThread {
                if (isResumed) {
                    loader.forceLoad()
                }
            }
        }

        internal fun reset() {
            retryCount = 0
            partialContentBar!!.visibility = View.INVISIBLE
        }

    }

}


private val log = TLog.get(DeviceListFragment::class.java)

private fun containsFilter(value: String?, query: String?): Boolean {
    return value != null && value.contains(query ?: "")
}

private fun helpfulOrderDeviceComparator(): Comparator<ParticleDevice> {
    val deviceOnlineStatusComparator = Comparator<ParticleDevice> { lhs, rhs ->
        BooleanComparator.getTrueFirstComparator()
            .compare(lhs.isConnected, rhs.isConnected)
    }
    val nullComparator = NullComparator<String>(false)
    val unnamedDevicesFirstComparator = Comparator<ParticleDevice> { lhs, rhs ->
        val lhname = lhs.name
        val rhname = rhs.name
        nullComparator.compare(lhname, rhname)
    }

    val chain: ComparatorChain<ParticleDevice>
    chain = ComparatorChain(deviceOnlineStatusComparator, false)
    chain.addComparator(unnamedDevicesFirstComparator, false)
    return chain
}
