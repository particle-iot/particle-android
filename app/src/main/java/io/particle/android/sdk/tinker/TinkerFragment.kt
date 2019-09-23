package io.particle.android.sdk.tinker

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle.State
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.particle.android.sdk.cloud.BroadcastContract
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.cloud.ParticleDevice.FunctionDoesNotExistException
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.android.sdk.tinker.DeviceUiState.ONLINE_USING_TINKER
import io.particle.android.sdk.ui.InspectorActivity
import io.particle.android.sdk.ui.flashTinkerWithDialog
import io.particle.android.sdk.utils.Async
import io.particle.android.sdk.utils.Prefs
import io.particle.android.sdk.utils.TLog
import io.particle.android.sdk.utils.pass
import io.particle.android.sdk.utils.ui.Ui
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.utils.safeToast
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_tinker.*
import kotlinx.coroutines.delay
import mu.KotlinLogging
import java.io.IOException


internal const val ANALOG_READ_MAX = 4095
internal const val ANALOG_WRITE_MAX_PWM = 255
internal const val ANALOG_WRITE_MAX_DAC = ANALOG_READ_MAX


/** A fragment representing a single Tinker screen. */
class TinkerFragment : Fragment(), OnClickListener {

    private val log = TLog.get(TinkerFragment::class.java)

    private var allPins: List<Pin> = mutableListOf()
    private var pinsByName: MutableMap<String, Pin> = arrayMapOf()
    private val devicesUpdatedListener = DevicesUpdatedListener()

    private val device: ParticleDevice
        get() { return (requireActivity() as InspectorActivity).device }
    private val api: TinkerApi by lazy { TinkerApi(this@TinkerFragment) }
    private lateinit var prefs: Prefs

    private var selectedPin: Pin? = null
    private var selectDialog: AlertDialog? = null

    private val pinInWriteMode: Pin?
        get() {
            for (pin in allPins) {
                if (pin.isAnalogWriteMode) {
                    return pin
                }
            }
            return null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        prefs = Prefs.getInstance(activity)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tinker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadViews()
        unmutePins()  // hack to make the pins show their functions correctly on first load

        setupListeners()

        if (TinkerPrefs.getInstance(requireActivity()).isFirstVisit) {
            instructions_container.isVisible = true
            fragmentManager?.commit { add(R.id.instructions_container, InstructionsFragment()) }
        }

        updateState()

        action_device_flash_tinker.setOnClickListener {
            flashTinkerWithDialog(
                requireActivity(),
                requireActivity().findViewById(R.id.inspector_tab_section),
                device
            )
            Scopes().onMain {
                delay(3000)
                if (isVisible && viewLifecycleOwner.lifecycle.currentState.isAtLeast(State.STARTED)) {
                    updateState()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(activity!!).registerReceiver(
            devicesUpdatedListener, devicesUpdatedListener.buildIntentFilter()
        )
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(activity!!).unregisterReceiver(
            devicesUpdatedListener
        )
    }

    private fun updateFromState(newUiState: DeviceUiState) {
        for (state in DeviceUiState.values()) {
            val contentView = view?.findViewById<View>(state.contentViewId)
            contentView?.isVisible = (state == newUiState)
        }

        if (newUiState != ONLINE_USING_TINKER) {
            muteAllPins()
        } else {
            unmutePins()
        }
    }

    private fun loadViews() {
        // This is kind of wrong, since the other enum value for this type is "NONE", which isn't
        // a function, but it seemed even more absurd to create a whole second enum where 3/4 of
        // the values are identical
        allPins = loadPins(device.deviceType!!, this)

        for (pin in allPins) {
            pinsByName[pin.name] = pin
            val pinFunction = prefs.getPinFunction(device.id, pin.name)
            pin.configuredAction = pinFunction
        }
    }

    private fun setupListeners() {
        // Set up pin listeners
        for (pin in allPins) {
            for (view in listOf(pin.pinLabelView, pin.pinLabelView.parent as ViewGroup)) {
                view.setOnClickListener { v ->
                    val writeModePin = pinInWriteMode
                    if (writeModePin != null && pin != selectedPin) {
                        writeModePin.showAnalogWriteValue()
                        unmutePins()
                        return@setOnClickListener
                    }
                    selectedPin = pin
                    onPinClick(pin)
                }

                view.setOnLongClickListener { v ->
                    val writeModePin = pinInWriteMode
                    if (writeModePin != null && pin != selectedPin) {
                        writeModePin.showAnalogWriteValue()
                        unmutePins()
                        return@setOnLongClickListener true
                    }

                    if (pin.configuredAction != PinAction.NONE) {
                        pin.configuredAction = PinAction.NONE
                        onFunctionSelected(pin, PinAction.NONE)
                    } else {
                        selectedPin = pin
                        showTinkerSelectFunctionDialog(pin)
                    }
                    true
                }
            }
        }

        // Set up other listeners
        tinker_main.setOnClickListener {
            for (pin in allPins) {
                if (pin.isAnalogWriteMode) {
                    pin.showAnalogWriteValue()
                }
            }
            unmutePins()
        }
    }

    private fun onPinClick(selectedPin: Pin) {
        if (selectedPin.configuredAction !== PinAction.NONE) {
            // Perform requested action
            when (selectedPin.configuredAction) {
                PinAction.ANALOG_READ -> doAnalogRead(selectedPin)
                PinAction.ANALOG_WRITE -> if (selectedPin.isAnalogWriteMode) {
                    selectedPin.showAnalogWriteValue()
                    unmutePins()
                } else {
                    doAnalogWrite(selectedPin)
                }
                PinAction.DIGITAL_READ -> doDigitalRead(selectedPin)
                PinAction.DIGITAL_WRITE -> doDigitalWrite(selectedPin)
                else -> pass
            }
        } else {
            showTinkerSelectFunctionDialog(selectedPin)
        }
    }

    private fun showTinkerSelectFunctionDialog(pin: Pin) {
        // No current action on the pin
        mutePinsExcept(pin)
//        toggleViewVisibilityWithFade(R.id.tinker_logo, false)

        val selectDialogView = requireActivity().layoutInflater.inflate(
            R.layout.tinker_select, view as ViewGroup?, false
        )

        selectDialog = AlertDialog.Builder(
            activity,
            R.style.Theme_MaterialComponents_Dialog_Alert
        )
            .setView(selectDialogView)
            .setCancelable(true)
            .setOnCancelListener { it.dismiss() }
            .create()

        val dialog = selectDialog!!
        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnDismissListener {
            unmutePins()
//            toggleViewVisibilityWithFade(R.id.tinker_logo, true)
            selectDialog = null
        }

        val buttonsAndColors = mapOf(
            R.id.tinker_button_analog_read to R.color.tinker_analog_read,
            R.id.tinker_button_analog_write to R.color.tinker_analog_write,
            R.id.tinker_button_digital_read to R.color.tinker_digital_read,
            R.id.tinker_button_digital_write to R.color.tinker_digital_write
        )

        val ctx = requireContext()
        for ((buttonId, colorId) in buttonsAndColors) {
            val outlineBg = ctx.getDrawable(R.drawable.device_filter_button_background_selected)!!
            outlineBg.mutate()
            val functionColor = ContextCompat.getColor(ctx, colorId)
            DrawableCompat.setTint(outlineBg, functionColor)
            val button = Ui.findView<View>(selectDialogView, buttonId)
            button.background = outlineBg
            button.setOnClickListener(this)
        }

        fun View.setVisible(shouldBeVisible: Boolean) {
            this.visibility = if (shouldBeVisible) View.VISIBLE else View.INVISIBLE
        }

        val analogRead = Ui.findView<View>(selectDialogView, R.id.tinker_button_analog_read)
        val analogWrite = Ui.findView<View>(selectDialogView, R.id.tinker_button_analog_write)
        val digitalRead = Ui.findView<View>(selectDialogView, R.id.tinker_button_digital_read)
        val digitalWrite = Ui.findView<View>(selectDialogView, R.id.tinker_button_digital_write)

        digitalRead.setVisible(pin.functions.contains(PinAction.DIGITAL_READ))
        digitalWrite.setVisible(pin.functions.contains(PinAction.DIGITAL_WRITE))
        analogRead.setVisible(pin.functions.contains(PinAction.ANALOG_READ))
        analogWrite.setVisible(
            PinAction.ANALOG_WRITE in pin.functions
                    || PinAction.ANALOG_WRITE_DAC in pin.functions
        )

        (selectDialogView.findViewById<View>(R.id.tinker_select_pin) as TextView).text = pin.label

        selectDialogView.findViewById<View>(R.id.p_action_close).setOnClickListener {
            dialog.dismiss()
        }

        selectDialog!!.show()
    }

    private fun setTinkerSelectButtonSelected(selectButtonView: View?, allButtons: List<View>) {
        for (button in allButtons) {
            Ui.findView<View>(button, R.id.tinker_button_color).visibility =
                if (button === selectButtonView) View.VISIBLE else View.INVISIBLE
            button.setBackgroundResource(
                if (button === selectButtonView)
                    R.color.tinker_selection_overlay_item_selected_bg
                else
                    R.color.tinker_selection_overlay_item_bg
            )
        }
    }

    private fun noIReallyMeanItIWantThisToBeTransparent(view: View) {
        if (view.id == R.id.tinker_select) {
            return
        }
        view.setBackgroundColor(0)
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                noIReallyMeanItIWantThisToBeTransparent(view.getChildAt(i))
            }
        }
    }

    private fun toggleViewVisibilityWithFade(viewId: Int, show: Boolean) {
        val view = Ui.findView<View>(this, viewId)
        val shortAnimTime = 150 // ms
        view.visibility = View.VISIBLE
        view.animate()
            .setDuration(shortAnimTime.toLong())
            .alpha((if (show) 1 else 0).toFloat())
            .setListener(object : AnimatorListenerAdapter() {

                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = if (show) View.VISIBLE else View.GONE
                }
            })
    }

    private fun mutePinsExcept(pin: Pin) {
        for (currentPin in allPins) {
            if (currentPin != pin) {
                currentPin.mute()
            }
        }
    }

    private fun muteAllPins() {
        for (pin in allPins) {
            pin.mute()
        }
    }

    private fun unmutePins() {
        // Unmute pins
        for (pin in allPins) {
            pin.unmute()
        }
    }

    private fun hideTinkerSelect() {
        // Reset tinker select dialog state
//        toggleViewVisibilityWithFade(R.id.tinker_logo, true)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.tinker_button_analog_read -> onFunctionSelected(selectedPin, PinAction.ANALOG_READ)
            R.id.tinker_button_analog_write -> onFunctionSelected(
                selectedPin,
                PinAction.ANALOG_WRITE
            )
            R.id.tinker_button_digital_read -> onFunctionSelected(
                selectedPin,
                PinAction.DIGITAL_READ
            )
            R.id.tinker_button_digital_write -> onFunctionSelected(
                selectedPin,
                PinAction.DIGITAL_WRITE
            )
        }// hideTinkerSelect();
    }

    private fun onFunctionSelected(selected: Pin?, action: PinAction) {
        selectDialog?.dismiss()
        selectDialog = null
//        toggleViewVisibilityWithFade(R.id.tinker_logo, true)

        selected?.reset()
        selected?.configuredAction = action
        selected?.let { prefs.savePinFunction(device.id, it.name, action) }
        selected?.updatePinColor()
        // FIXME: should this actually be commented out?
        //		 hideTinkerSelect();
        //		 unmutePins();
    }

    private fun doAnalogRead(pin: Pin) {
        pin.animateYourself()

        api.read(PinStuff(pin.name, PinAction.ANALOG_READ, pin.analogValue))
        // pin.showAnalogRead(850);
    }

    private fun doAnalogWrite(pin: Pin) {
        mutePinsExcept(pin)
//        toggleViewVisibilityWithFade(R.id.tinker_logo, false)
        pin.showAnalogWrite(object : OnAnalogWriteListener {
            override fun onAnalogWrite(value: Int) {
                for (pin1 in allPins) {
                    if (pin1.isAnalogWriteMode) {
                        pin1.showAnalogWriteValue()
                    }
                }
                unmutePins()
                hideTinkerSelect()
                pin.animateYourself()
                pin.showAnalogValue(value)
                api.write(PinStuff(pin.name, PinAction.ANALOG_WRITE, pin.analogValue), value)
            }
        })
    }

    private fun doDigitalRead(pin: Pin) {
        pin.animateYourself()

        api.read(PinStuff(pin.name, PinAction.DIGITAL_READ, pin.digitalValue!!.intValue))
        // pin.showDigitalRead(DigitalValue.HIGH);
    }

    private fun doDigitalWrite(pin: Pin) {
        pin.animateYourself()
        val currentValue = pin.digitalValue
        val newValue = if (currentValue === DigitalValue.HIGH) {
            DigitalValue.LOW
        } else {
            DigitalValue.HIGH
        }
        api.write(
            PinStuff(pin.name, PinAction.DIGITAL_WRITE, currentValue!!.intValue),
            newValue.intValue
        )
        // pin.showDigitalWrite(newValue);
    }

    private fun onTinkerCallComplete(stuff: PinStuff, valueToApply: Int) {
        log.d("onTinkerCallComplete()")

        val pin = pinsByName[stuff.pinName]

        if (pin!!.configuredAction === PinAction.NONE) {
            // received a response for a pin that has since been cleared
            pin!!.stopAnimating()
            return
        }

        if (stuff.pinAction === PinAction.ANALOG_READ || stuff.pinAction === PinAction.ANALOG_WRITE) {
            pin!!.showAnalogValue(valueToApply)
        } else {
            pin!!.showDigitalRead(DigitalValue.fromInt(valueToApply))
        }
    }


    abstract class TinkerWork(
        private val stuff: PinStuff,
        private val tinkerFragment: TinkerFragment
    ) : Async.ApiWork<ParticleDevice, Int>() {

        override fun onSuccess(returnValue: Int) {
            tinkerFragment.onTinkerCallComplete(stuff, returnValue)
        }

        override fun onFailure(exception: ParticleCloudException) {
            tinkerFragment.onTinkerCallComplete(stuff, stuff.currentValue)
            // FIXME: do real error handling!
            //			ErrorsDelegate errorsDelegate = ((BaseActivity) getActivity()).getErrorsDelegate();
            //			errorsDelegate.showTinkerError();
        }

    }


    private class TinkerApi(
        private val tinkerFragment: TinkerFragment
    ) {

        private val actionToFunctionName: MutableMap<PinAction, String>

        private val log = KotlinLogging.logger {}

        init {
            actionToFunctionName = ArrayMap(4)
            actionToFunctionName[PinAction.ANALOG_READ] = "analogread"
            actionToFunctionName[PinAction.ANALOG_WRITE] = "analogwrite"
            actionToFunctionName[PinAction.DIGITAL_READ] = "digitalread"
            actionToFunctionName[PinAction.DIGITAL_WRITE] = "digitalwrite"
        }

        internal fun write(stuff: PinStuff, newValue: Int) {
            try {
                Async.executeAsync(tinkerFragment.device, object : TinkerWork(stuff, tinkerFragment) {
                    @Throws(ParticleCloudException::class, IOException::class)
                    override fun callApi(sparkDevice: ParticleDevice): Int? {

                        val stringValue: String = if (stuff.pinAction === PinAction.ANALOG_WRITE) {
                            newValue.toString()
                        } else {
                            if (newValue == DigitalValue.HIGH.intValue) "HIGH" else "LOW"
                        }

                        return try {
                            val result = sparkDevice.callFunction(
                                actionToFunctionName[stuff.pinAction]!!,
                                listOf(stuff.pinName, stringValue))

                            if (result  == 1) {
                                newValue
                            } else {
                                stuff.currentValue
                            }

                        } catch (e: FunctionDoesNotExistException) {
                            e.message?.let { tinkerFragment.activity.safeToast(it) }
                            stuff.currentValue // it didn't change
                        } catch (e: ParticleCloudException) {
                            e.message?.let { tinkerFragment.activity.safeToast(it) }
                            stuff.currentValue // it didn't change
                        }
                    }
                })
            } catch (e: ParticleCloudException) {
                // should we show a message here? ignore it all together?
            }

        }

        internal fun read(stuff: PinStuff) {
            try {
                Async.executeAsync(tinkerFragment.device, object : TinkerWork(stuff, tinkerFragment) {
                    @Throws(ParticleCloudException::class, IOException::class)
                    override fun callApi(sparkDevice: ParticleDevice): Int? {

                        return try {
                            sparkDevice.callFunction(
                                actionToFunctionName[stuff.pinAction]!!,
                                listOf(stuff.pinName)
                            )
                        } catch (e: FunctionDoesNotExistException) {
                            e.message?.let { tinkerFragment.activity.safeToast(it) }
                            stuff.currentValue // it didn't change
                        } catch (e: ParticleCloudException) {
                            tinkerFragment.activity.safeToast(e.message)
                            stuff.currentValue
                        }
                    }
                })
            } catch (e: ParticleCloudException) {
                // should we show a message here? ignore it all together?
            }

        }
    }

    private fun updateState() {
        updateFromState(device.uiState)
    }


    private inner class DevicesUpdatedListener : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            updateState()
        }

        internal fun buildIntentFilter(): IntentFilter {
            return IntentFilter(BroadcastContract.BROADCAST_DEVICES_UPDATED)
        }
    }

}

data class PinStuff(
    val pinName: String,
    val pinAction: PinAction,
    val currentValue: Int
)


// Doing this as a fragment because I ran into touch issues doing it as just a pinLabelView,
// and because this gives us back button support at no additional charge.
class InstructionsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.tinker_instructions, container, false)
        v.setOnClickListener {
            TinkerPrefs.getInstance(activity!!).setVisited(true)
            fragmentManager?.commit { remove(this@InstructionsFragment) }
        }
        return v
    }
}
