package io.particle.android.sdk.ui.devicelist

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.core.os.postDelayed
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProviders
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.android.sdk.ui.BaseActivity
import io.particle.android.sdk.updateUsernameWithCrashlytics
import io.particle.android.sdk.utils.SoftAPConfigRemover
import io.particle.android.sdk.utils.WifiFacade
import io.particle.android.sdk.utils.ui.Ui
import io.particle.mesh.ui.setup.MeshSetupActivity
import io.particle.sdk.app.R


class DeviceListActivity : BaseActivity() {

    private var softAPConfigRemover: SoftAPConfigRemover? = null
    private lateinit var filterViewModel: DeviceFilterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateUsernameWithCrashlytics(ParticleCloudSDK.getCloud().loggedInUsername)

        setContentView(R.layout.activity_device_list)

        filterViewModel = ViewModelProviders.of(this).get(DeviceFilterViewModel::class.java)
        filterViewModel.refreshDevices()

        softAPConfigRemover = SoftAPConfigRemover(this, WifiFacade.get(this))

        // TODO: If exposing deep links into your app, handle intents here.

        if (Ui.findFrag<Fragment>(this, R.id.fragment_parent) == null) {
            supportFragmentManager.commit {
                add(R.id.fragment_parent, DeviceListFragment.newInstance())
            }
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
        val currentFragment = Ui.findFrag<Fragment>(this, R.id.fragment_parent)
        var deviceList: DeviceListFragment? = null
        if (currentFragment is DeviceListFragment) {
            deviceList = currentFragment
        }
        if (deviceList?.onBackPressed() != true) {
            super.onBackPressed()
        }
    }
}
