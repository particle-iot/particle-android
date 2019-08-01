package io.particle.android.sdk.ui.devicelist

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.SearchView
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.appbar.CollapsingToolbarLayout
import io.particle.android.sdk.accountsetup.LoginActivity
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.ui.BaseActivity
import io.particle.android.sdk.utils.Py.list
import io.particle.android.sdk.utils.SoftAPConfigRemover
import io.particle.android.sdk.utils.WifiFacade
import io.particle.android.sdk.utils.ui.Ui
import io.particle.mesh.ui.setup.MeshSetupActivity
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.activity_device_list.*
import pl.brightinventions.slf4android.LogRecord
import pl.brightinventions.slf4android.NotifyDeveloperDialogDisplayActivity
import java.util.*


class DeviceListActivity : BaseActivity(), DeviceListFragment.Callbacks {

    private var isAppBarExpanded = false
    private var softAPConfigRemover: SoftAPConfigRemover? = null
    private var deviceList: DeviceListFragment? = null

    private var searchView: SearchView? = null

    private val offsetChangedListener: OnOffsetChangedListener =
        OnOffsetChangedListener { appBarLayout: AppBarLayout, verticalOffset: Int ->
            isAppBarExpanded = Math.abs(verticalOffset) != appBarLayout.totalScrollRange

            if (!isAppBarExpanded && appBarLayout.isActivated) {
                lockAppBarClosed()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)

        // set up filter listeners
        val checkboxes = listOf(
            photonFilter,
            electronFilter,
            coreFilter,
            raspberryFilter,
            p1Filter,
            redBearFilter,
            oakFilter,
            bluzFilter,
            xenonFilter,
            argonFilter,
            boronFilter
        )
        for (cb: CheckBox in checkboxes) {
            cb.setOnCheckedChangeListener { _, _  ->  onDeviceTypeFilterChanged() }
        }

        softAPConfigRemover = SoftAPConfigRemover(this, WifiFacade.get(this))

        deviceList = Ui.findFrag(this, R.id.fragment_device_list)
        // TODO: If exposing deep links into your app, handle intents here.

        // Show the Up button in the action bar.
        setSupportActionBar(Ui.findView(this, R.id.toolbar))
        val supportActionBar = supportActionBar
        if (supportActionBar != null) {
            val background =
                ContextCompat.getDrawable(this, R.drawable.ic_triangy_toolbar_background)

            val toolBar = Ui.findView<CollapsingToolbarLayout>(this, R.id.collapsing_toolbar)
            toolBar.background = background

            appbar.addOnOffsetChangedListener(offsetChangedListener)
            appbar.setExpanded(false)
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
        appbar.removeOnOffsetChangedListener(offsetChangedListener)
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (isAppBarExpanded) {
            appbar.setExpanded(false)
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
        //on show of search view hide titleRes and logout menu option
        searchView!!.setOnSearchClickListener {
            title.visibility = View.GONE
            logoutItem.isVisible = false
            filterItem.isVisible = false
            searchView!!.requestFocus()
        }
        //on collapse of search bar show titleRes and logout menu option
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

    private fun onDeviceTypeFilterChanged() {
        val typeArrayList = ArrayList<ParticleDeviceType?>().apply {
            add(if (photonFilter.isChecked) ParticleDeviceType.PHOTON else null)
            add(if (electronFilter.isChecked) ParticleDeviceType.ELECTRON else null)
            add(if (coreFilter.isChecked) ParticleDeviceType.CORE else null)
            add(if (raspberryFilter.isChecked) ParticleDeviceType.RASPBERRY_PI else null)
            add(if (p1Filter.isChecked) ParticleDeviceType.P1 else null)
            add(if (redBearFilter.isChecked) ParticleDeviceType.RED_BEAR_DUO else null)
            add(if (oakFilter.isChecked) ParticleDeviceType.DIGISTUMP_OAK else null)
            add(if (bluzFilter.isChecked) ParticleDeviceType.BLUZ else null)
            add(if (xenonFilter.isChecked) ParticleDeviceType.XENON else null)
            add(if (argonFilter.isChecked) ParticleDeviceType.ARGON else null)
            add(if (boronFilter.isChecked) ParticleDeviceType.BORON else null)
        }
        deviceList!!.filter(typeArrayList)
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
                .setPositiveButton(R.string.log_out) { dialog, _ ->
                    val cloud = ParticleCloudSDK.getCloud()
                    cloud.logOut()
                    startActivity(Intent(this@DeviceListActivity, LoginActivity::class.java))
                    finish()

                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()
            R.id.action_filter -> {
                unlockAppBarOpen()
                appbar.setExpanded(!isAppBarExpanded)
            }
            R.id.action_send_logs -> sendLogs()
        }
        return super.onOptionsItemSelected(item)
    }

    //region DeviceListFragment.Callbacks
    override fun onDeviceSelected(device: ParticleDevice) {
        // was used for two-pane mode, but we no longer support that.
    }
    //endregion

    private fun lockAppBarClosed() {
        appbar.setExpanded(false, false)
        appbar.isActivated = false
        val lp = appbar.layoutParams as CoordinatorLayout.LayoutParams
        val tv = TypedValue()

        if (theme.resolveAttribute(R.attr.actionBarSize, tv, true)) {
            lp.height = TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        }
    }

    private fun unlockAppBarOpen() {
        appbar.setExpanded(true, false)
        appbar.isActivated = true
        appbar.layoutParams = CoordinatorLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
