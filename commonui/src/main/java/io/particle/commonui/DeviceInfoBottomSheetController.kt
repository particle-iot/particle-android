package io.particle.commonui

import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.CompoundButton
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
import io.particle.commonui.databinding.ViewDeviceInfoBinding
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
    private val binding = ViewDeviceInfoBinding.bind(root)
    private val lastHeardDateFormat = SimpleDateFormat("MMM d, yyyy, h:mm a")

    private val log = KotlinLogging.logger {}

    // ~15fps "breathing" glow per status dot (see BreathingGlow), reused across updates.
    private val statusDotGlows = mutableMapOf<ImageView, BreathingGlow>()

    fun initializeBottomSheet() {
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {

            override fun onResume(owner: LifecycleOwner) {
                updateDeviceDetails()
            }

            override fun onStop(owner: LifecycleOwner) {
                binding.actionSignalDevice.isChecked = false
            }

        })

        binding.actionSignalDevice.setOnCheckedChangeListener(::onSignalSwitchChanged)
        binding.actionDeviceRename.setOnClickListener {
            RenameHelper.renameDevice(activity, device)
        }
        binding.actionEditDeviceNotes.setOnClickListener {
            val liveData = MutableLiveData<String>()
            DeviceNotesDelegate.editDeviceNotes(activity, device, scopes, liveData)
            liveData.observe(activity, Observer {
                binding.notes.setText(it)
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
        binding.expandedHandle.setOnClickListener(toggleOnTapListener)
        binding.collapsedExpander.setOnClickListener(toggleOnTapListener)
        binding.actionPingDevice.setOnClickListener { onPingClicked() }

        if (device.deviceType!!.toConnectivityType() != Gen3ConnectivityType.CELLULAR) {
            binding.iccidWrapper.isVisible = false
            binding.dataUsageWrapper.isVisible = false
            binding.dataLimitWrapper.isVisible = false
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
                binding.dataUsage.text = dataUsage?.let { "$dataUsage MB used" }
                binding.dataLimit.text = dataLimit?.let { "$dataLimit MB per month" }
            }
        }
    }

    fun updateDeviceDetails() {
        binding.deviceType.styleAsPill(device.deviceType!!)
        binding.collapsedDevicePill.styleAsPill(device.deviceType!!)
        binding.productImage.setImageResource(device.deviceType!!.productImage)
        binding.deviceName.text = device.name
        binding.deviceId.text = device.id.toUpperCase()
        binding.serial.text = device.serialNumber
        binding.osVersion.text = device.version ?: "(Unknown)"
        binding.lastHandshake.text = device.lastHeard?.let { lastHeardDateFormat.format(it) } ?: "(Unknown)"
        // FIXME: add notes editing functionality
        binding.notes.setText(device.notes)
        binding.iccid.text = device.iccid

        setUpStatusDotAndText(device.isConnected)
    }

    private fun initAnimations() {
        val mutators = mutableListOf(
            Mutator(
                binding.collapsedExpander,
                listOf(FADE),
                ShownWhen.COLLAPSED
            ),

            Mutator(
                binding.onlineStatusDotCollapsed,
                listOf(FADE),
                ShownWhen.COLLAPSED
            ),

            Mutator(
                binding.collapsedDevicePill,
                listOf(FADE, RESIZE_HEIGHT),
                ShownWhen.COLLAPSED
            )
        )

        mutators.addAll(
            listOf(
                binding.expandedHandle,
                binding.actionDeviceRename,
                binding.onlineStatusText,
                binding.onlineStatusDot,
                binding.actionPingDevice,
                binding.actionSignalDevice
            ).map { Mutator(it, listOf(FADE, RESIZE_HEIGHT)) }
        )

        mutators.add(
            Mutator(binding.productImage, listOf(RESIZE_WIDTH))
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
        binding.onlineStatusText.text = if (isOnline) "Online" else "Offline"

        fun setUpDot(imageView: ImageView) {
            imageView.setImageResource(getStatusColoredDot(device, isOnline))

            val glow = statusDotGlows.getOrPut(imageView) { BreathingGlow(imageView) }
            if (isOnline) glow.start() else glow.stop()
        }

        for (dotView in listOf(binding.onlineStatusDot, binding.onlineStatusDotCollapsed)) {
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
            binding.actionPingDevice.isEnabled = false
            binding.pingProgressBar.isVisible = true

            val online = scopes.withWorker {
                try {
                    return@withWorker device.pingDevice()
                } catch (ex: Exception) {
                    return@withWorker null
                }
            }

            binding.pingProgressBar.isVisible = false
            binding.actionPingDevice.isEnabled = true
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
            try {
                val device = cloud.getDevice(deviceId)
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
