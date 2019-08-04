package io.particle.android.sdk.ui.devicelist

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import io.particle.android.sdk.accountsetup.LoginActivity
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.ui.BaseActivity
import io.particle.android.sdk.utils.Py.list
import io.particle.android.sdk.utils.SoftAPConfigRemover
import io.particle.android.sdk.utils.WifiFacade
import io.particle.android.sdk.utils.ui.Ui
import io.particle.mesh.ui.setup.MeshSetupActivity
import io.particle.sdk.app.R
import pl.brightinventions.slf4android.LogRecord
import pl.brightinventions.slf4android.NotifyDeveloperDialogDisplayActivity


class DeviceListActivity : BaseActivity(), DeviceListFragment.Callbacks {

    private var softAPConfigRemover: SoftAPConfigRemover? = null
    private var deviceList: DeviceListFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)

        softAPConfigRemover = SoftAPConfigRemover(this, WifiFacade.get(this))

        deviceList = Ui.findFrag(this, R.id.fragment_device_list)
        // TODO: If exposing deep links into your app, handle intents here.

        if (Ui.findFrag<Fragment>(this, R.id.fragment_parent) == null) {
            supportFragmentManager.commit { add(R.id.fragment_parent, DeviceListFragment()) }
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

    override fun onBackPressed() {
        if (deviceList?.onBackPressed() == false) {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.device_list, menu)
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
                .setPositiveButton(R.string.log_out) { dialog, _ ->
                    val cloud = ParticleCloudSDK.getCloud()
                    cloud.logOut()
                    startActivity(Intent(this@DeviceListActivity, LoginActivity::class.java))
                    finish()

                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()
            R.id.action_send_logs -> sendLogs()
        }
        return super.onOptionsItemSelected(item)
    }

    //region DeviceListFragment.Callbacks
    override fun onDeviceSelected(device: ParticleDevice) {
        // was used for two-pane mode, but we no longer support that.
    }
    //endregion
}
