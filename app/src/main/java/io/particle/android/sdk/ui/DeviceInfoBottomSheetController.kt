package io.particle.android.sdk.ui

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.CompoundButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.ui.MutatorOp.FADE
import io.particle.android.sdk.ui.MutatorOp.RESIZE_HEIGHT
import io.particle.android.sdk.ui.MutatorOp.RESIZE_WIDTH
import io.particle.android.sdk.ui.ShownWhen.EXPANDED
import io.particle.mesh.setup.flow.FlowRunnerSystemInterface
import io.particle.mesh.setup.flow.Scopes
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.view_device_info.view.*
import mu.KotlinLogging
import java.text.SimpleDateFormat


class DeviceInfoBottomSheetController(
    private val activity: FragmentActivity,
    private val scopes: Scopes,
    private val root: ConstraintLayout,
    private val device: ParticleDevice,
    // FIXME:
    private val systemInterface: FlowRunnerSystemInterface?
) {

    private val behavior = BottomSheetBehavior.from(root)
    private val lastHeardDateFormat = SimpleDateFormat("MMM d, yyyy, h:mm a")

    private val log = KotlinLogging.logger {}

    fun initializeBottomSheet() {
        root.action_signal_device.setOnCheckedChangeListener(::onSignalSwitchChanged)
        root.action_device_rename.setOnClickListener {
            RenameHelper.renameDevice(activity, device)
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

        initAnimations()
    }

    fun updateDeviceDetails() {
        val productName = root.context.getString(device.deviceType!!.productName)
        root.device_type.text = productName
        root.collapsed_device_type.text = productName
        root.product_image.setImageResource(device.deviceType!!.productImage)
        root.device_name.text = device.name
        root.online_status_text.text = if (device.isConnected) "Online" else "Offline"
        root.device_id.text = device.id.toUpperCase()
        root.serial.text = device.serialNumber
        root.os_version.text = device.version ?: "(Unknown)"
        root.last_handshake.text = lastHeardDateFormat.format(device.lastHeard)
        // FIXME: add notes editing functionality
        root.notes.setText(device.notes)

        setUpStatusDot(device.isConnected)
    }

    private fun initAnimations() {
        val mutators = mutableListOf(
            Mutator(
                root.collapsed_expander,
                listOf(FADE),
                ShownWhen.COLLAPSED
            ),

            Mutator(
                root.collapsed_device_type,
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

    private fun setUpStatusDot(isOnline: Boolean) {
        val statusDot = root.online_status_dot
        statusDot.setImageResource(getStatusColoredDot(device, isOnline))
        val animFade = AnimationUtils.loadAnimation(root.context, R.anim.fade_in_out)
        if (isOnline) {
            statusDot.startAnimation(animFade)
        }
    }

    private fun getStatusColoredDot(device: ParticleDevice, isOnline: Boolean): Int {
        return if (device.isFlashing) {
            R.drawable.device_flashing_dot

        } else if (isOnline) {
            if (device.isRunningTinker) {
                R.drawable.online_dot
            } else {
                R.drawable.online_non_tinker_dot
            }

        } else {
            R.drawable.offline_dot
        }
    }

    private fun onPingClicked() {
        scopes.onWorker {
            val online = try {
                systemInterface?.showGlobalProgressSpinner(true)
                device.pingDevice()
            } catch (ex: Exception) {
                // FIXME: show error feedback here?
                null
            } finally {
                systemInterface?.showGlobalProgressSpinner(false)
            }

            scopes.onMain { online?.let { setUpStatusDot(it) } }
        }
    }

    private fun onSignalSwitchChanged(button: CompoundButton, isChecked: Boolean) {
        val deviceId = device.id
        scopes.onWorker {
            val cloud = ParticleCloudSDK.getCloud()
            val device = cloud.getDevice(deviceId)
            try {
                device.startStopSignaling(isChecked)
            } catch (ex: Exception) {
                log.error(ex) { "Error turning rainbow-shouting ${if (isChecked) "ON" else "OFF"}" }
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

private val fadeInterpolator = AccelerateDecelerateInterpolator()
//AccelerateInterpolator