package io.particle.android.sdk.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ARGON
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.A_SERIES
import io.particle.android.sdk.cloud.ParticleEventVisibility
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.android.sdk.cloud.models.DeviceStateChange
import io.particle.android.sdk.controlpanel.ControlPanelActivity
import io.particle.android.sdk.utils.Async
import io.particle.android.sdk.utils.ui.Ui
import io.particle.sdk.app.R
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe


/** An activity representing the Inspector screen for a Device */
class InspectorActivity : BaseActivity() {

    private val syncStatus = object : Runnable {
        override fun run() {
            invalidateOptionsMenu()
            handler.postDelayed(this, 1000 * 60L)
        }
    }

    private var device: ParticleDevice? = null
    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inspector)

        // Show the Up button in the action bar.
        val supportActionBar = supportActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.device_inspector)

        device = intent.getParcelableExtra(EXTRA_DEVICE)
        val deviceNameView = Ui.findView<TextView>(this, R.id.deviceName)
        deviceNameView.text = device!!.name

        val deviceStatus = Ui.findView<ImageView>(this, R.id.deviceStatus)
        val animFade = AnimationUtils.loadAnimation(this, R.anim.fade_in_out)
        deviceStatus.startAnimation(animFade)
        deviceStatus.setImageResource(getStatusColoredDot(device!!))

        setupInspectorPages()
        handler.postDelayed(syncStatus, 1000 * 60L)
    }

    public override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
        try {
            device!!.subscribeToSystemEvents()
        } catch (ignore: ParticleCloudException) {
            //minor issue if we don't update online/offline states
        }

    }

    public override fun onPause() {
        EventBus.getDefault().unregister(this)
        try {
            device!!.unsubscribeFromSystemEvents()
        } catch (ignore: ParticleCloudException) {
        }

        super.onPause()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            android.R.id.home -> finish()
            R.id.action_event_publish -> presentPublishDialog()
            R.id.action_launchcontrol_panel -> device?.let {
                startActivity(ControlPanelActivity.buildIntent(this, it.id))
            }
            else -> {
                val actionId = item.itemId

                return DeviceActionsHelper.takeActionForDevice(actionId, this, device) ||
                        DeviceMenuUrlHandler.handleActionItem(this, actionId, item.title) ||
                        super.onOptionsItemSelected(item)
            }
        }

        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.inspector, menu)

        val type = device!!.deviceType
        if (type === ParticleDeviceType.ARGON
            || type === ParticleDeviceType.BORON
            || type === ParticleDeviceType.XENON
        ) {
            menu.findItem(R.id.action_device_flash_tinker).isVisible = false
        }

        if (type !in listOf(ARGON, A_SERIES)) {
            menu.findItem(R.id.action_launchcontrol_panel).isVisible = false
        }

        return true
    }

    @Subscribe
    fun onEvent(device: ParticleDevice) {
        //update device and UI
        //TODO update more fields
        this.device = device
        val deviceNameView = Ui.findView<TextView>(this, R.id.deviceName)
        deviceNameView.post { deviceNameView.text = device.name }
    }

    @Subscribe
    fun onEvent(deviceStateChange: DeviceStateChange) {
        //reload menu to display online/offline
        invalidateOptionsMenu()
    }

    private fun getStatusColoredDot(device: ParticleDevice): Int {
        return if (device.isFlashing) {
            R.drawable.device_flashing_dot
        } else if (device.isConnected) {
            if (device.isRunningTinker) {
                R.drawable.online_dot

            } else {
                R.drawable.online_non_tinker_dot
            }

        } else {
            R.drawable.offline_dot
        }
    }

    private fun setupInspectorPages() {
        val viewPager = findViewById<ViewPager>(R.id.viewPager)
        viewPager.adapter = InspectorPager(supportFragmentManager, device)
        viewPager.offscreenPageLimit = 3
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        tabLayout.setupWithViewPager(viewPager)
        //hiding keyboard on tab changed
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {}

            override fun onPageScrollStateChanged(state: Int) {
                val view = currentFocus
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                if (view != null && imm != null) {
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
        })
    }


    private fun presentPublishDialog() {
        val publishDialogView = View.inflate(this, R.layout.publish_event, null)

        AlertDialog.Builder(
            this,
            R.style.ParticleSetupTheme_DialogNoDimBackground
        )
            .setView(publishDialogView)
            .setPositiveButton(R.string.publish_positive_action) { _, _ ->
                val nameView = Ui.findView<TextView>(publishDialogView, R.id.eventName)
                val valueView = Ui.findView<TextView>(publishDialogView, R.id.eventValue)
                val privateEventRadio =
                    Ui.findView<RadioButton>(publishDialogView, R.id.privateEvent)

                val name = nameView.text.toString()
                val value = valueView.text.toString()
                val eventVisibility = if (privateEventRadio.isChecked)
                    ParticleEventVisibility.PRIVATE
                else
                    ParticleEventVisibility.PUBLIC

                publishEvent(name, value, eventVisibility)
            }
            .setNegativeButton(R.string.cancel, null)
            .setCancelable(true)
            .setOnCancelListener { it.dismiss() }
            .show()
    }

    private fun publishEvent(name: String, value: String, eventVisibility: Int) {
        try {
            Async.executeAsync(device!!, object : Async.ApiProcedure<ParticleDevice>() {
                @Throws(ParticleCloudException::class)
                override fun callApi(particleDevice: ParticleDevice): Void? {
                    particleDevice.cloud.publishEvent(name, value, eventVisibility, 600)
                    return null
                }

                override fun onFailure(exception: ParticleCloudException) {
                    Toast.makeText(
                        this@InspectorActivity, "Failed to publish '" + name +
                                "' event", Toast.LENGTH_SHORT
                    ).show()
                }
            })
        } catch (e: ParticleCloudException) {
            Toast.makeText(
                this, "Failed to publish '" + name +
                        "' event", Toast.LENGTH_SHORT
            ).show()
        }

    }

    companion object {
        val EXTRA_DEVICE = "EXTRA_DEVICE"

        fun buildIntent(ctx: Context, device: ParticleDevice): Intent {
            return Intent(ctx, InspectorActivity::class.java)
                .putExtra(EXTRA_DEVICE, device)
        }
    }
}