package io.particle.commonui

import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.CompoundButton
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.commonui.MutatorOp.FADE
import io.particle.commonui.MutatorOp.RESIZE_HEIGHT
import io.particle.commonui.MutatorOp.RESIZE_WIDTH
import io.particle.commonui.ShownWhen.EXPANDED
import io.particle.mesh.setup.flow.Gen3ConnectivityType
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.toConnectivityType
import kotlinx.android.synthetic.main.view_device_info.view.*
import mu.KotlinLogging
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat


class DeviceInfoBottomSheetController(
    private val activity: AppCompatActivity,
    private val scopes: Scopes,
    private val root: ConstraintLayout,
    var device: ParticleDevice
) {

    var sheetBehaviorState: Int
        get() = behavior.state
        set(value) {
            behavior.state = value
        }

    private val behavior = BottomSheetBehavior.from(root)
    private val lastHeardDateFormat = SimpleDateFormat("MMM d, yyyy, h:mm a")

    private val log = KotlinLogging.logger {}

    fun initializeBottomSheet() {
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {

            override fun onResume(owner: LifecycleOwner) {
                updateDeviceDetails()
            }

            override fun onStop(owner: LifecycleOwner) {
                root.action_signal_device.isChecked = false
            }

        })

        root.action_signal_device.setOnCheckedChangeListener(::onSignalSwitchChanged)
        root.action_device_rename.setOnClickListener {
            RenameHelper.renameDevice(activity, device)
        }
        root.action_edit_device_notes.setOnClickListener {
            val liveData = MutableLiveData<String>()
            DeviceNotesDelegate.editDeviceNotes(activity, device, scopes, liveData)
            liveData.observe(activity, Observer {
                root.notes.setText(it)
            })
        }

        val toggleOnTapListener = View.OnClickListener {
            when (behavior.state) {
                BottomSheetBehavior.STATE_EXPANDED -> {
                    behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
                BottomSheetBehavior.STATE_COLLAPSED -> {
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
        root.expanded_handle.setOnClickListener(toggleOnTapListener)
        root.collapsed_expander.setOnClickListener(toggleOnTapListener)
        root.action_ping_device.setOnClickListener { onPingClicked() }

        if (device.deviceType!!.toConnectivityType() != Gen3ConnectivityType.CELLULAR) {
            root.iccid_wrapper.isVisible = false
            root.data_usage_wrapper.isVisible = false
            root.data_limit_wrapper.isVisible = false
        } else {
            initCellularDataFields()
        }

        initAnimations()
    }

    private fun initCellularDataFields() {
        scopes.onWorker {
            val dataUsage: Float
            val dataLimit: Int?
            try {
                dataUsage = device.getCurrentDataUsage()
                dataLimit = device.iccid?.let {
                    val sim = device.cloud.getSim(it)
                    return@let sim.monthlyDataRateLimitInMBs
                }
            } catch (ex: Exception) {
                return@onWorker
            }

            scopes.onMain {
                root.data_usage.text = dataUsage?.let { "$dataUsage MB used" }
                root.data_limit.text = dataLimit?.let { "$dataLimit MB per month" }
            }
        }
    }

    fun updateDeviceDetails() {
        root.device_type.styleAsPill(device.deviceType!!)
        root.collapsed_device_pill.styleAsPill(device.deviceType!!)
        root.product_image.setImageResource(device.deviceType!!.productImage)
        root.device_name.text = device.name
        root.device_id.text = device.id.toUpperCase()
        root.serial.text = device.serialNumber
        root.os_version.text = device.version ?: "(Unknown)"
        root.last_handshake.text = device.lastHeard?.let { lastHeardDateFormat.format(it) } ?: "(Unknown)"
        // FIXME: add notes editing functionality
        root.notes.setText(device.notes)
        root.iccid.text = device.iccid

        setUpStatusDotAndText(device.isConnected)
    }

    private fun initAnimations() {
        val mutators = mutableListOf(
            Mutator(
                root.collapsed_expander,
                listOf(FADE),
                ShownWhen.COLLAPSED
            ),

            Mutator(
                root.online_status_dot_collapsed,
                listOf(FADE),
                ShownWhen.COLLAPSED
            ),

            Mutator(
                root.collapsed_device_pill,
                listOf(FADE, RESIZE_HEIGHT),
                ShownWhen.COLLAPSED
            )
        )

        mutators.addAll(
            listOf(
                root.expanded_handle,
                root.action_device_rename,
                root.online_status_text,
                root.online_status_dot,
                root.action_ping_device,
                root.action_signal_device
            ).map { Mutator(it, listOf(FADE, RESIZE_HEIGHT)) }
        )

        mutators.add(
            Mutator(root.product_image, listOf(RESIZE_WIDTH))
        )

        val cb = object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) { /* no-op */
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                mutators.forEach { it.mutate(slideOffset) }
            }
        }

        root.doOnNextLayout {
            mutators.forEach { it.setInitialValues() }
            // run onSlide() using the collapsed value (0.0) to get everything sized/faded correctly
            cb.onSlide(root, 0.0f)
        }

        behavior.setBottomSheetCallback(cb)
    }

    private fun setUpStatusDotAndText(isOnline: Boolean) {
        root.online_status_text.text = if (isOnline) "Online" else "Offline"

        fun setUpDot(imageView: ImageView) {
            imageView.setImageResource(getStatusColoredDot(device, isOnline))

            imageView.animation?.cancel()
            if (isOnline) {
                val animFade = AnimationUtils.loadAnimation(root.context, R.anim.fade_in_out)
                imageView.startAnimation(animFade)
            }
        }

        for (dotView in listOf(root.online_status_dot, root.online_status_dot_collapsed)) {
            setUpDot(dotView)
        }
    }

    private fun getStatusColoredDot(device: ParticleDevice, isOnline: Boolean): Int {
        return if (device.isFlashing) {
            R.drawable.device_status_dot_flashing

        } else if (isOnline) {
            if (device.isRunningTinker) {
                R.drawable.device_status_dot_online_tinker
            } else {
                R.drawable.device_status_dot_online_non_tinker
            }

        } else {
            R.drawable.device_status_dot_offline
        }
    }

    private fun onPingClicked() {
        scopes.onMain {
            root.action_ping_device.isEnabled = false
            root.ping_progress_bar.isVisible = true

            val online = scopes.withWorker {
                try {
                    return@withWorker device.pingDevice()
                } catch (ex: Exception) {
                    return@withWorker null
                }
            }

            root.ping_progress_bar.isVisible = false
            root.action_ping_device.isEnabled = true
            online?.let { setUpStatusDotAndText(it) }
        }
    }

    private fun onSignalSwitchChanged(button: CompoundButton?, isChecked: Boolean) {
        shouldDeviceSignal(button, isChecked)
    }

    private fun shouldDeviceSignal(button: CompoundButton?, shouldSignal: Boolean) {
        val buttonRef = WeakReference(button)
        val deviceId = device.id
        scopes.onWorker {
            val cloud = ParticleCloudSDK.getCloud()
            val device = cloud.getDevice(deviceId)
            try {
                device.startStopSignaling(shouldSignal)
            } catch (ex: Exception) {
                log.error(ex) { "Error turning rainbows ${if (shouldSignal) "ON" else "OFF"}" }
                scopes.onMain {
                    buttonRef.get()?.isChecked = false
                }
            }
        }
    }

}


private enum class ShownWhen {
    COLLAPSED,
    EXPANDED
}


private enum class MutatorOp {
    FADE,
    RESIZE_WIDTH,
    RESIZE_HEIGHT
}


private class Mutator(
    private val view: View,
    private val mutatorOps: List<MutatorOp>,
    private val shownWhen: ShownWhen = EXPANDED
) {

    private var initialHeight: Int = Int.MIN_VALUE
    private var initialWidth: Int = Int.MIN_VALUE
    private var minWidth: Int = Int.MIN_VALUE

    fun setInitialValues() {
        initialHeight = view.measuredHeight
        initialWidth = view.measuredWidth
        minWidth = view.minimumWidth
    }

    fun mutate(slideOffset: Float) {
        val scaleValue = if (shownWhen == EXPANDED) slideOffset else (1.0f - slideOffset)
        for (op in mutatorOps) {
            doMutate(scaleValue, op)
        }
    }

    private fun doMutate(slideOffset: Float, mutatorOp: MutatorOp) {
        when (mutatorOp) {
            RESIZE_HEIGHT -> resizeHeight(slideOffset)
            RESIZE_WIDTH -> resizeWidth(slideOffset)
            FADE -> view.alpha = fadeInterpolator.getInterpolation(slideOffset)
        }
    }

    private fun resizeWidth(slideOffset: Float) {
        val calculatedWidth = (initialWidth * slideOffset).toInt()
        val newWidth = maxOf(calculatedWidth, minWidth)
        val params = view.layoutParams
        params.width = newWidth
        view.layoutParams = params
    }

    private fun resizeHeight(slideOffset: Float) {
        val calculatedHeight = (initialHeight * slideOffset).toInt()
        val newHeight = maxOf(calculatedHeight, 1)
        val params = view.layoutParams
        params.height = newHeight
        view.layoutParams = params
    }

}

private val fadeInterpolator = AccelerateInterpolator() //AccelerateDecelerateInterpolator()
//AccelerateInterpolator
