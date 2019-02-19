package io.particle.android.sdk.ui

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.SearchView

import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout

import java.util.ArrayList
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnCheckedChanged
import io.particle.android.sdk.accountsetup.LoginActivity
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.tinker.TinkerFragment
import io.particle.android.sdk.utils.SoftAPConfigRemover
import io.particle.android.sdk.utils.WifiFacade
import io.particle.android.sdk.utils.ui.Ui
import io.particle.mesh.setup.ui.MeshSetupActivity
import io.particle.sdk.app.R
import pl.brightinventions.slf4android.LogRecord
import pl.brightinventions.slf4android.NotifyDeveloperDialogDisplayActivity

import io.particle.android.sdk.utils.Py.list

/**
 * An activity representing a list of Devices. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [TinkerActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 *
 *
 * The activity makes heavy use of fragments. The list of items is a
 * [DeviceListFragment] and the item details
 * (if present) is a [TinkerFragment].
 *
 *
 * This activity also implements the required
 * [DeviceListFragment.Callbacks] interface
 * to listen for item selections.
 */
class DeviceListActivity : BaseActivity(), DeviceListFragment.Callbacks {

    // Whether or not the activity is in two-pane mode, i.e. running on a tablet
    private var mTwoPane: Boolean = false
    private var isAppBarExpanded = false
    private var softAPConfigRemover: SoftAPConfigRemover? = null
    private var deviceList: DeviceListFragment? = null

    @BindView(R.id.photonFilter)
    var photonFilter: CheckBox? = null
    @BindView(R.id.electronFilter)
    var electronFilter: CheckBox? = null
    @BindView(R.id.coreFilter)
    var coreFilter: CheckBox? = null
    @BindView(R.id.raspberryFilter)
    var raspberryFilter: CheckBox? = null
    @BindView(R.id.p1Filter)
    var p1Filter: CheckBox? = null
    @BindView(R.id.redBearFilter)
    var redBearFilter: CheckBox? = null
    @BindView(R.id.bluzFilter)
    var bluzFilter: CheckBox? = null
    @BindView(R.id.oakFilter)
    var oakFilter: CheckBox? = null
    @BindView(R.id.xenonFilter)
    var xenonFilter: CheckBox? = null
    @BindView(R.id.argonFilter)
    var argonFilter: CheckBox? = null
    @BindView(R.id.boronFilter)
    var boronFilter: CheckBox? = null
    @BindView(R.id.appbar)
    var appBarLayout: AppBarLayout? = null
    private var searchView: SearchView? = null

    private val offsetChangedListener = { appBarLayout, verticalOffset ->
        isAppBarExpanded = Math.abs(verticalOffset) != appBarLayout.getTotalScrollRange()

        if (!isAppBarExpanded && appBarLayout.isActivated()) {
            lockAppBarClosed()
        }
    }

    @OnCheckedChanged(
        R.id.photonFilter,
        R.id.electronFilter,
        R.id.coreFilter,
        R.id.raspberryFilter,
        R.id.p1Filter,
        R.id.redBearFilter,
        R.id.bluzFilter,
        R.id.oakFilter,
        R.id.xenonFilter,
        R.id.argonFilter,
        R.id.boronFilter
    )
    fun onDeviceType() {
        val typeArrayList = ArrayList<ParticleDevice.ParticleDeviceType>()
        typeArrayList.add(if (photonFilter!!.isChecked) ParticleDevice.ParticleDeviceType.PHOTON else null)
        typeArrayList.add(if (electronFilter!!.isChecked) ParticleDevice.ParticleDeviceType.ELECTRON else null)
        typeArrayList.add(if (coreFilter!!.isChecked) ParticleDevice.ParticleDeviceType.CORE else null)
        typeArrayList.add(if (raspberryFilter!!.isChecked) ParticleDevice.ParticleDeviceType.RASPBERRY_PI else null)
        typeArrayList.add(if (p1Filter!!.isChecked) ParticleDevice.ParticleDeviceType.P1 else null)
        typeArrayList.add(if (redBearFilter!!.isChecked) ParticleDevice.ParticleDeviceType.RED_BEAR_DUO else null)
        typeArrayList.add(if (oakFilter!!.isChecked) ParticleDevice.ParticleDeviceType.DIGISTUMP_OAK else null)
        typeArrayList.add(if (bluzFilter!!.isChecked) ParticleDevice.ParticleDeviceType.BLUZ else null)
        typeArrayList.add(if (xenonFilter!!.isChecked) ParticleDeviceType.XENON else null)
        typeArrayList.add(if (argonFilter!!.isChecked) ParticleDeviceType.ARGON else null)
        typeArrayList.add(if (boronFilter!!.isChecked) ParticleDeviceType.BORON else null)

        deviceList!!.filter(typeArrayList)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)
        ButterKnife.bind(this)

        softAPConfigRemover = SoftAPConfigRemover(this, WifiFacade.get(this))

        if (Ui.findView<View>(this, R.id.device_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            // FIXME: need to impl in RecyclerView if we want two-pane mode
            //            ((DeviceListFragment) getSupportFragmentManager()
            //                    .findFragmentById(R.id.device_list))
            //                    .setActivateOnItemClick(true);
        }

        deviceList = Ui.findFrag(this, R.id.fragment_device_list)
        // TODO: If exposing deep links into your app, handle intents here.

        // Show the Up button in the action bar.
        setSupportActionBar(Ui.findView<Toolbar>(this, R.id.toolbar))
        val supportActionBar = supportActionBar
        if (supportActionBar != null) {
            val background =
                ContextCompat.getDrawable(this, R.drawable.ic_triangy_toolbar_background)

            val collapsingToolbar =
                Ui.findView<CollapsingToolbarLayout>(this, R.id.collapsing_toolbar)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                collapsingToolbar.background = background
            } else {
                collapsingToolbar.setBackgroundDrawable(background)
            }

            appBarLayout!!.addOnOffsetChangedListener(offsetChangedListener)
            appBarLayout!!.setExpanded(false)
            lockAppBarClosed()
        }

        onProcessIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        onProcessIntent(intent)
    }

    private fun onProcessIntent(intent: Intent) {
        val intentUri = intent.data
        // we have to do all this nonsense because Branch sends us garbage URIs
        // which have *two schemes* in them.
        if (intentUri != null
            && intentUri.encodedPath != null
            && (intentUri.encodedPath!!.contains("meshsetup") || intentUri.host != null && intentUri.host!!.contains(
                "meshsetup"
            ))
        ) {
            startActivity(Intent(this, MeshSetupActivity::class.java))
        }
    }


    override fun onStart() {
        super.onStart()
        softAPConfigRemover!!.removeAllSoftApConfigs()
        softAPConfigRemover!!.reenableWifiNetworks()
    }

    override fun onDestroy() {
        appBarLayout!!.removeOnOffsetChangedListener(offsetChangedListener)
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (isAppBarExpanded) {
            appBarLayout!!.setExpanded(false)
        } else if (!searchView!!.isIconified) {
            searchView!!.isIconified = true
        } else if (deviceList == null || !deviceList!!.onBackPressed()) {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.device_list, menu)
        val logoutItem = menu.findItem(R.id.action_log_out)
        val searchItem = menu.findItem(R.id.action_search)
        val filterItem = menu.findItem(R.id.action_filter)
        val title = Ui.findView<View>(this, android.R.id.title)

        searchView = searchItem.actionView as SearchView
        searchView!!.setQuery(deviceList!!.textFilter, false)
        //on show of search view hide title and logout menu option
        searchView!!.setOnSearchClickListener { v ->
            title.visibility = View.GONE
            logoutItem.isVisible = false
            filterItem.isVisible = false
            searchView!!.requestFocus()
        }
        //on collapse of search bar show title and logout menu option
        searchView!!.setOnCloseListener {
            title.visibility = View.VISIBLE
            logoutItem.isVisible = true
            filterItem.isVisible = true
            false
        }
        searchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                deviceList!!.filter(query)
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                deviceList!!.filter(newText)
                return false
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    private fun sendLogs() {
        val lr = LogRecord(java.util.logging.Level.WARNING, "")

        NotifyDeveloperDialogDisplayActivity.showDialogIn(
            this,
            lr,
            list(),
            "Logs from the Particle Android app",
            "",
            list("pl.brightinventions.slf4android.ReadLogcatEntriesAsyncTask")
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_log_out -> AlertDialog.Builder(this)
                .setMessage(R.string.logout_confirm_message)
                .setPositiveButton(R.string.log_out) { dialog, which ->
                    val cloud = ParticleCloudSDK.getCloud()
                    cloud.logOut()
                    startActivity(Intent(this@DeviceListActivity, LoginActivity::class.java))
                    finish()

                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel) { dialog, which -> dialog.dismiss() }
                .show()
            R.id.action_filter -> {
                unlockAppBarOpen()
                appBarLayout!!.setExpanded(!isAppBarExpanded)
            }
            R.id.action_send_logs -> sendLogs()
        }
        return super.onOptionsItemSelected(item)
    }

    //region DeviceListFragment.Callbacks
    override fun onDeviceSelected(device: ParticleDevice) {
        // FIXME: re-enable
        //
        //        return;
        //
        //        if (mTwoPane) {
        //            // In two-pane mode, show the detail view in this activity by
        //            // adding or replacing the detail fragment using a
        //            // fragment transaction.
        //            getSupportFragmentManager()
        //                    .beginTransaction()
        //                    .replace(R.id.device_detail_container, TinkerFragment.newInstance(id))
        //                    .commit();
        //
        //        } else {
        // In single-pane mode, simply start the detail activity
        // for the selected item.
        startActivity(TinkerActivity.buildIntent(this, device))
        //        }
    }
    //endregion

    private fun lockAppBarClosed() {
        appBarLayout!!.setExpanded(false, false)
        appBarLayout!!.isActivated = false
        val lp = appBarLayout!!.layoutParams as CoordinatorLayout.LayoutParams
        val tv = TypedValue()

        if (theme.resolveAttribute(R.attr.actionBarSize, tv, true)) {
            lp.height = TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        }
    }

    private fun unlockAppBarOpen() {
        appBarLayout!!.setExpanded(true, false)
        appBarLayout!!.isActivated = true
        appBarLayout!!.layoutParams = CoordinatorLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
