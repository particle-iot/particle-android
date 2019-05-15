package io.particle.android.sdk.tinker

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.*
import android.view.View.OnClickListener
import android.widget.TextView
import androidx.collection.ArrayMap
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.particle.android.sdk.cloud.BroadcastContract
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.android.sdk.ui.DeviceActionsHelper
import io.particle.android.sdk.ui.DeviceMenuUrlHandler
import io.particle.android.sdk.utils.Async
import io.particle.android.sdk.utils.Prefs
import io.particle.android.sdk.utils.Py.list
import io.particle.android.sdk.utils.Py.map
import io.particle.android.sdk.utils.TLog
import io.particle.android.sdk.utils.Toaster
import io.particle.android.sdk.utils.ui.Ui
import io.particle.sdk.app.R
import java.io.IOException
import java.util.*


// The device that this fragment represents
private const val ARG_DEVICE = "ARG_DEVICE"
// The device that this fragment represents
private const val STATE_DEVICE = "STATE_DEVICE"

private const val ANALOG_READ_MAX = 4095
private const val ANALOG_WRITE_MAX = 255
private const val ANALOG_WRITE_MAX_ALT = ANALOG_READ_MAX

/**
 * A fragment representing a single Tinker screen.
 */
class TinkerFragment : Fragment(), OnClickListener {

    companion object {

        fun newInstance(device: ParticleDevice): TinkerFragment {
            return TinkerFragment().apply {
                arguments = bundleOf(ARG_DEVICE to device)
            }
        }
    }

    private val log = TLog.get(TinkerFragment::class.java)

    private val allPins = list<Pin>()
    private val pinsByName = map<String, Pin>()

    private var selectedPin: Pin? = null
    private var selectDialog: AlertDialog? = null
    private var device: ParticleDevice? = null
    private var api: TinkerApi? = null
    private var prefs: Prefs? = null

    private val devicesUpdatedListener = DevicesUpdatedListener()

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
        if (savedInstanceState != null) {
            device = savedInstanceState.getParcelable(STATE_DEVICE)
        } else {
            device = arguments!!.getParcelable(ARG_DEVICE)
        }
        api = TinkerApi()
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
        setupListeners()

        if (TinkerPrefs.getInstance(activity).isFirstVisit) {
            fragmentManager!!.beginTransaction()
                .add(R.id.instructions_container, InstructionsFragment())
                .addToBackStack("InstructionsFragment_TRANSACTION")
                .commit()
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_DEVICE, device)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        // we handle both the context device row actions here and our own
        //        inflater.inflate(R.menu.context_device_row, menu);
        inflater!!.inflate(R.menu.tinker, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val actionId = item!!.itemId
        if (DeviceActionsHelper.takeActionForDevice(actionId, activity, device)) {
            return true

        } else if (actionId == R.id.action_device_clear_tinker) {
            prefs!!.clearTinker(device!!.id)
            for (pin in allPins) {
                pin.configuredAction = PinAction.NONE
                pin.reset()
            }
            return true

        } else {
            return DeviceMenuUrlHandler.handleActionItem(
                activity,
                actionId,
                item.title
            ) || super.onOptionsItemSelected(item)
        }
    }

    private fun findPinView(id: Int): TextView {
        return Ui.findView(this, id)
    }

    private fun loadViews() {
        // This is kind of wrong, since the other enum value for this type is "NONE", which isn't
        // a function, but it seemed even more absurd to create a whole second enum where 3/4 of
        // the values are identical
        val allFunctions = EnumSet.of(
            PinAction.ANALOG_READ,
            PinAction.ANALOG_WRITE,
            PinAction.DIGITAL_READ,
            PinAction.DIGITAL_WRITE
        )

        val noAnalogWrite = EnumSet.of(
            PinAction.ANALOG_READ,
            PinAction.DIGITAL_READ,
            PinAction.DIGITAL_WRITE
        )

        val noAnalogRead = EnumSet.of(
            PinAction.ANALOG_WRITE,
            PinAction.DIGITAL_READ,
            PinAction.DIGITAL_WRITE
        )

        val digitalOnly = EnumSet.of(
            PinAction.DIGITAL_READ,
            PinAction.DIGITAL_WRITE
        )

        when (device!!.deviceType) {
            ParticleDevice.ParticleDeviceType.CORE -> {
                allPins.add(Pin(findPinView(R.id.tinker_a0), PinType.A, "A0", allFunctions))
                allPins.add(Pin(findPinView(R.id.tinker_a1), PinType.A, "A1", allFunctions))
                allPins.add(Pin(findPinView(R.id.tinker_a2), PinType.A, "A2", noAnalogWrite))
                allPins.add(Pin(findPinView(R.id.tinker_a3), PinType.A, "A3", noAnalogWrite))
                allPins.add(Pin(findPinView(R.id.tinker_a4), PinType.A, "A4", allFunctions))
                allPins.add(Pin(findPinView(R.id.tinker_a5), PinType.A, "A5", allFunctions))
                allPins.add(Pin(findPinView(R.id.tinker_a6), PinType.A, "A6", allFunctions))
                allPins.add(Pin(findPinView(R.id.tinker_a7), PinType.A, "A7", allFunctions))

                allPins.add(Pin(findPinView(R.id.tinker_d0), PinType.D, "D0", noAnalogRead))
                allPins.add(Pin(findPinView(R.id.tinker_d1), PinType.D, "D1", noAnalogRead))
                allPins.add(Pin(findPinView(R.id.tinker_d2), PinType.D, "D2", digitalOnly))
                allPins.add(Pin(findPinView(R.id.tinker_d3), PinType.D, "D3", digitalOnly))
                allPins.add(Pin(findPinView(R.id.tinker_d4), PinType.D, "D4", digitalOnly))
                allPins.add(Pin(findPinView(R.id.tinker_d5), PinType.D, "D5", digitalOnly))
                allPins.add(Pin(findPinView(R.id.tinker_d6), PinType.D, "D6", digitalOnly))
                allPins.add(Pin(findPinView(R.id.tinker_d7), PinType.D, "D7", digitalOnly))
            }

            // default: Photon/"cuppa"
            ParticleDevice.ParticleDeviceType.PHOTON -> {
                val allFunctionsDAC = EnumSet.of(
                    PinAction.ANALOG_READ,
                    PinAction.ANALOG_WRITE_DAC,
                    PinAction.DIGITAL_READ,
                    PinAction.DIGITAL_WRITE
                )

                allPins.add(Pin(findPinView(R.id.tinker_a0), PinType.A, "A0", noAnalogWrite))
                allPins.add(Pin(findPinView(R.id.tinker_a1), PinType.A, "A1", noAnalogWrite))
                allPins.add(Pin(findPinView(R.id.tinker_a2), PinType.A, "A2", noAnalogWrite))
                allPins.add(Pin(findPinView(R.id.tinker_a3), PinType.A, "A3", allFunctionsDAC, "A3", ANALOG_WRITE_MAX_ALT))

                // (II) Analog write duplicated to value in D3 (mention in UI)
                allPins.add(Pin(findPinView(R.id.tinker_a4), PinType.A, "A4", allFunctions))

                // (I) Analog write duplicated to value in D2 (mention in UI)
                allPins.add(Pin(findPinView(R.id.tinker_a5), PinType.A, "A5", allFunctions))

                allPins.add(Pin(findPinView(R.id.tinker_a6), PinType.A, "A6", allFunctionsDAC, "DAC", ANALOG_WRITE_MAX_ALT))
                allPins.add(Pin(findPinView(R.id.tinker_a7), PinType.A, "A7", allFunctions, "WKP", ANALOG_WRITE_MAX))

                allPins.add(Pin(findPinView(R.id.tinker_d0), PinType.D, "D0", noAnalogRead))
                allPins.add(Pin(findPinView(R.id.tinker_d1), PinType.D, "D1", noAnalogRead))
                allPins.add(Pin(findPinView(R.id.tinker_d2), PinType.D, "D2", noAnalogRead))

                // (II) Analog write duplicated to value in A3 (mention in UI)
                allPins.add(Pin(findPinView(R.id.tinker_d3), PinType.D, "D3", noAnalogRead))

                // (II) Analog write duplicated to value in A4 (mention in UI)
                allPins.add(Pin(findPinView(R.id.tinker_d4), PinType.D, "D4", digitalOnly))

                allPins.add(Pin(findPinView(R.id.tinker_d5), PinType.D, "D5", digitalOnly))
                allPins.add(Pin(findPinView(R.id.tinker_d6), PinType.D, "D6", digitalOnly))
                allPins.add(Pin(findPinView(R.id.tinker_d7), PinType.D, "D7", digitalOnly))
            }
            
            else -> {
                val allFunctionsDAC = EnumSet.of(
                    PinAction.ANALOG_READ,
                    PinAction.ANALOG_WRITE_DAC,
                    PinAction.DIGITAL_READ,
                    PinAction.DIGITAL_WRITE
                )
                allPins.add(Pin(findPinView(R.id.tinker_a0), PinType.A, "A0", noAnalogWrite))
                allPins.add(Pin(findPinView(R.id.tinker_a1), PinType.A, "A1", noAnalogWrite))
                allPins.add(Pin(findPinView(R.id.tinker_a2), PinType.A, "A2", noAnalogWrite))
                allPins.add(
                    Pin(
                        findPinView(R.id.tinker_a3),
                        PinType.A,
                        "A3",
                        allFunctionsDAC,
                        "A3",
                        ANALOG_WRITE_MAX_ALT
                    )
                )
                allPins.add(Pin(findPinView(R.id.tinker_a4), PinType.A, "A4", allFunctions))
                allPins.add(Pin(findPinView(R.id.tinker_a5), PinType.A, "A5", allFunctions))
                allPins.add(
                    Pin(
                        findPinView(R.id.tinker_a6),
                        PinType.A,
                        "A6",
                        allFunctionsDAC,
                        "DAC",
                        ANALOG_WRITE_MAX_ALT
                    )
                )
                allPins.add(
                    Pin(
                        findPinView(R.id.tinker_a7),
                        PinType.A,
                        "A7",
                        allFunctions,
                        "WKP",
                        ANALOG_WRITE_MAX
                    )
                )
                allPins.add(Pin(findPinView(R.id.tinker_d0), PinType.D, "D0", noAnalogRead))
                allPins.add(Pin(findPinView(R.id.tinker_d1), PinType.D, "D1", noAnalogRead))
                allPins.add(Pin(findPinView(R.id.tinker_d2), PinType.D, "D2", noAnalogRead))
                allPins.add(Pin(findPinView(R.id.tinker_d3), PinType.D, "D3", noAnalogRead))
                allPins.add(Pin(findPinView(R.id.tinker_d4), PinType.D, "D4", digitalOnly))
                allPins.add(Pin(findPinView(R.id.tinker_d5), PinType.D, "D5", digitalOnly))
                allPins.add(Pin(findPinView(R.id.tinker_d6), PinType.D, "D6", digitalOnly))
                allPins.add(Pin(findPinView(R.id.tinker_d7), PinType.D, "D7", digitalOnly))
            }
        }

        for (pin in allPins) {
            pinsByName[pin.name] = pin
            val pinFunction = prefs!!.getPinFunction(device!!.id, pin.name)
            pin.configuredAction = pinFunction
        }
    }

    private fun setupListeners() {
        // Set up pin listeners
        for (pin in allPins) {
            for (view in list(pin.view, pin.view.parent as ViewGroup)) {
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
                    selectedPin = pin
                    showTinkerSelect(pin)
                    true
                }
            }
        }

        // Set up other listeners
        Ui.findView<View>(this, R.id.tinker_main).setOnClickListener(this)
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
                else -> {
                }
            }
        } else {
            showTinkerSelect(selectedPin)
        }
    }

    private fun showTinkerSelect(pin: Pin) {
        // No current action on the pin
        mutePinsExcept(pin)
        toggleViewVisibilityWithFade(R.id.tinker_logo, false)

        val selectDialogView = activity!!.layoutInflater.inflate(
            R.layout.tinker_select, view as ViewGroup?, false
        )

        selectDialog = AlertDialog.Builder(
            activity,
            R.style.ParticleSetupTheme_DialogNoDimBackground
        )
            .setView(selectDialogView)
            .setCancelable(true)
            .setOnCancelListener { it.dismiss() }
            .create()
        selectDialog!!.setCanceledOnTouchOutside(true)
        selectDialog!!.setOnDismissListener { dialog ->
            unmutePins()
            toggleViewVisibilityWithFade(R.id.tinker_logo, true)
            selectDialog = null
        }

        val analogRead = Ui.findView<View>(selectDialogView, R.id.tinker_button_analog_read)
        val analogWrite = Ui.findView<View>(selectDialogView, R.id.tinker_button_analog_write)
        val digitalRead = Ui.findView<View>(selectDialogView, R.id.tinker_button_digital_read)
        val digitalWrite = Ui.findView<View>(selectDialogView, R.id.tinker_button_digital_write)
        val allButtons = list(analogRead, analogWrite, digitalRead, digitalWrite)

        analogRead.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                setTinkerSelectButtonSelected(analogRead, allButtons)
            }
            false
        }

        analogWrite.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                setTinkerSelectButtonSelected(analogWrite, allButtons)
            }
            false
        }

        digitalRead.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                setTinkerSelectButtonSelected(digitalRead, allButtons)
            }
            false
        }

        digitalWrite.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                setTinkerSelectButtonSelected(digitalWrite, allButtons)
            }
            false
        }

        digitalRead.setOnClickListener(this)
        digitalWrite.setOnClickListener(this)
        analogRead.setOnClickListener(this)
        analogWrite.setOnClickListener(this)

        setVisible(digitalRead, pin.functions.contains(PinAction.DIGITAL_READ))
        setVisible(digitalWrite, pin.functions.contains(PinAction.DIGITAL_WRITE))
        setVisible(analogRead, pin.functions.contains(PinAction.ANALOG_READ))
        setVisible(
            analogWrite,
            (pin.functions.contains(PinAction.ANALOG_WRITE)
                    || pin.functions.contains(PinAction.ANALOG_WRITE_DAC))
        )

        (selectDialogView.findViewById<View>(R.id.tinker_select_pin) as TextView).text = pin.label

        when (pin.configuredAction) {
            PinAction.ANALOG_READ -> setTinkerSelectButtonSelected(analogRead, allButtons)

            PinAction.ANALOG_WRITE_DAC, PinAction.ANALOG_WRITE -> setTinkerSelectButtonSelected(
                analogWrite,
                allButtons
            )

            PinAction.DIGITAL_READ -> setTinkerSelectButtonSelected(digitalRead, allButtons)

            PinAction.DIGITAL_WRITE -> setTinkerSelectButtonSelected(digitalWrite, allButtons)

            PinAction.NONE -> setTinkerSelectButtonSelected(null, allButtons)
        }

        selectDialog!!.show()

        val decorView = selectDialog!!.window!!.decorView
        noIReallyMeanItIWantThisToBeTransparent(decorView)
    }

    private fun setVisible(view: View, shouldBeVisible: Boolean) {
        view.visibility = if (shouldBeVisible) View.VISIBLE else View.INVISIBLE
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

    private fun unmutePins() {
        // Unmute pins
        for (pin in allPins) {
            pin.unmute()
        }
    }

    private fun hideTinkerSelect() {
        // Reset tinker select dialog state
        toggleViewVisibilityWithFade(R.id.tinker_logo, true)
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
            R.id.tinker_main -> {
                for (pin in allPins) {
                    if (pin.isAnalogWriteMode) {
                        pin.showAnalogWriteValue()
                    }
                }
                unmutePins()
            }
        }// hideTinkerSelect();
    }

    private fun onFunctionSelected(selectedPin: Pin?, action: PinAction) {
        if (selectDialog != null) {
            selectDialog!!.dismiss()
            selectDialog = null
        }
        toggleViewVisibilityWithFade(R.id.tinker_logo, true)

        selectedPin!!.reset()
        selectedPin.configuredAction = action
        prefs!!.savePinFunction(device!!.id, selectedPin.name, action)
        // FIXME: should this actually be commented out?
        //		 hideTinkerSelect();
        //		 unmutePins();
    }

    private fun doAnalogRead(pin: Pin) {
        pin.animateYourself()
        api!!.read(PinStuff(pin.name, PinAction.ANALOG_READ, pin.analogValue))
        // pin.showAnalogRead(850);
    }

    private fun doAnalogWrite(pin: Pin) {
        mutePinsExcept(pin)
        toggleViewVisibilityWithFade(R.id.tinker_logo, false)
        pin.showAnalogWrite { value ->
            for (pin1 in allPins) {
                if (pin1.isAnalogWriteMode) {
                    pin1.showAnalogWriteValue()
                }
            }
            unmutePins()
            hideTinkerSelect()
            pin.animateYourself()
            pin.showAnalogValue(value)
            api!!.write(PinStuff(pin.name, PinAction.ANALOG_WRITE, pin.analogValue), value)
        }
    }

    private fun doDigitalRead(pin: Pin) {
        pin.animateYourself()
        api!!.read(PinStuff(pin.name, PinAction.DIGITAL_READ, pin.digitalValue.intValue))
        // pin.showDigitalRead(DigitalValue.HIGH);
    }

    private fun doDigitalWrite(pin: Pin) {
        pin.animateYourself()
        val currentValue = pin.digitalValue
        val newValue = if (currentValue === DigitalValue.HIGH)
            DigitalValue.LOW
        else
            DigitalValue.HIGH
        api!!.write(
            PinStuff(pin.name, PinAction.DIGITAL_WRITE, currentValue.intValue),
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


    private abstract inner class TinkerWork internal constructor(internal val stuff: PinStuff) :
        Async.ApiWork<ParticleDevice, Int>() {

        override fun onFailure(exception: ParticleCloudException) {
            onTinkerCallComplete(stuff, stuff.currentValue)
            // FIXME: do real error handling!
            //			ErrorsDelegate errorsDelegate = ((BaseActivity) getActivity()).getErrorsDelegate();
            //			errorsDelegate.showTinkerError();
        }

    }


    private inner class TinkerApi internal constructor() {

        private val actionToFunctionName: MutableMap<PinAction, String>

        init {
            actionToFunctionName = ArrayMap(4)
            actionToFunctionName[PinAction.ANALOG_READ] = "analogread"
            actionToFunctionName[PinAction.ANALOG_WRITE] = "analogwrite"
            actionToFunctionName[PinAction.DIGITAL_READ] = "digitalread"
            actionToFunctionName[PinAction.DIGITAL_WRITE] = "digitalwrite"
        }

        internal fun write(stuff: PinStuff, newValue: Int) {
            try {
                Async.executeAsync(device!!, object : TinkerWork(stuff) {
                    @Throws(ParticleCloudException::class, IOException::class)
                    override fun callApi(sparkDevice: ParticleDevice): Int? {
                        val stringValue: String
                        if (stuff.pinAction === PinAction.ANALOG_WRITE) {
                            stringValue = newValue.toString()
                        } else {
                            stringValue =
                                if (newValue == DigitalValue.HIGH.intValue) "HIGH" else "LOW"
                        }
                        try {
                            return if (sparkDevice.callFunction(
                                    actionToFunctionName[stuff.pinAction]!!,
                                    list(stuff.pinName, stringValue)
                                ) == 1
                            )
                                newValue
                            else
                                stuff.currentValue
                        } catch (e: ParticleDevice.FunctionDoesNotExistException) {
                            Toaster.s(activity!!, e.message)
                            return stuff.currentValue // it didn't change
                        }

                    }

                    override fun onSuccess(returnValue: Int) {
                        onTinkerCallComplete(stuff, returnValue)
                    }
                })
            } catch (e: ParticleCloudException) {
                // should we show a message here? ignore it all together?
            }

        }

        internal fun read(stuff: PinStuff) {
            try {
                Async.executeAsync(device!!, object : TinkerWork(stuff) {
                    @Throws(ParticleCloudException::class, IOException::class)
                    override fun callApi(sparkDevice: ParticleDevice): Int? {
                        try {
                            return sparkDevice.callFunction(
                                actionToFunctionName[stuff.pinAction]!!,
                                list(stuff.pinName)
                            )
                        } catch (e: ParticleDevice.FunctionDoesNotExistException) {
                            Toaster.s(activity!!, e.message)
                            return stuff.currentValue
                        }

                    }

                    override fun onSuccess(returnValue: Int) {
                        onTinkerCallComplete(stuff, returnValue)
                    }
                })
            } catch (e: ParticleCloudException) {
                // should we show a message here? ignore it all together?
            }

        }
    }


    private inner class DevicesUpdatedListener : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {}

        internal fun buildIntentFilter(): IntentFilter {
            return IntentFilter(BroadcastContract.BROADCAST_DEVICES_UPDATED)
        }
    }


    // FIXME: rename to something more descriptive
    private class PinStuff internal constructor(
        internal val pinName: String,
        internal val pinAction: PinAction,
        internal val currentValue: Int
    ) {

        override fun toString(): String {
            return "PinStuff{" +
                    "pinName='" + pinName + '\''.toString() +
                    ", pinAction=" + pinAction +
                    ", currentValue=" + currentValue +
                    '}'.toString()
        }
    }


    // Doing this as a fragment because I ran into touch issues doing it as just a view,
    // and because this gives us back button support at no additional charge.
    class InstructionsFragment : Fragment() {

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val v = inflater.inflate(R.layout.tinker_instructions, container, false)
            v.setOnClickListener {
                TinkerPrefs.getInstance(activity).setVisited(true)
                activity!!.supportFragmentManager.popBackStack()
            }
            return v
        }
    }

}

