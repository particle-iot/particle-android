package io.particle.android.sdk.tinker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.ArrayMap;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.f2prateek.bundler.FragmentBundlerCompat;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import io.particle.android.sdk.cloud.BroadcastContract;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.tinker.Pin.OnAnalogWriteListener;
import io.particle.android.sdk.ui.DeviceActionsHelper;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Prefs;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.Toaster;
import io.particle.android.sdk.utils.ui.Ui;
import io.particle.android.sdk.utils.ui.WebViewActivity;
import io.particle.sdk.app.R;

import static io.particle.android.sdk.utils.Py.list;
import static io.particle.android.sdk.utils.Py.map;
import static io.particle.android.sdk.utils.Py.truthy;


/**
 * A fragment representing a single Core detail screen. This fragment is either
 * contained in a {@link CoreListActivity} in two-pane mode (on tablets) or a
 * {@link CoreDetailActivity} on handsets.
 */
public class TinkerFragment extends Fragment implements OnClickListener {

    public static TinkerFragment newInstance(ParticleDevice device) {
        return FragmentBundlerCompat.create(new TinkerFragment())
                .put(TinkerFragment.ARG_DEVICE, device)
                .build();
    }

    // The device that this fragment represents
    public static final String ARG_DEVICE = "ARG_DEVICE";

    private static final TLog log = TLog.get(TinkerFragment.class);

    // The device that this fragment represents
    private static final String STATE_DEVICE = "STATE_DEVICE";

    private static final int ANALOG_READ_MAX = 4095;
    private static final int ANALOG_WRITE_MAX = 255;
    private static final int ANALOG_WRITE_MAX_ALT = ANALOG_READ_MAX;


    private List<Pin> allPins = list();
    private Map<String, Pin> pinsByName = map();

    private Pin selectedPin;
    private AlertDialog selectDialog;
    private ParticleDevice device;
    private TinkerApi api;
    private Prefs prefs;

    private DevicesUpdatedListener devicesUpdatedListener = new DevicesUpdatedListener();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        prefs = Prefs.getInstance(getActivity());
        if (savedInstanceState != null) {
            device = savedInstanceState.getParcelable(STATE_DEVICE);
        } else {
            device = getArguments().getParcelable(ARG_DEVICE);
        }
        api = new TinkerApi();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tinker, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadViews();
        setupListeners();

        if (TinkerPrefs.getInstance(getActivity()).isFirstVisit()) {
            getFragmentManager().beginTransaction()
                    .add(R.id.instructions_container, new InstructionsFragment())
                    .addToBackStack("InstructionsFragment_TRANSACTION")
                    .commit();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        updateTitle();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                devicesUpdatedListener, devicesUpdatedListener.buildIntentFilter());
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(
                devicesUpdatedListener);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_DEVICE, device);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // we handle both the context device row actions here and our own
        inflater.inflate(R.menu.context_device_row, menu);
        inflater.inflate(R.menu.tinker, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int actionId = item.getItemId();
        if (DeviceActionsHelper.takeActionForDevice(actionId, getActivity(), device)) {
            return true;

        } else if (actionId == R.id.action_device_clear_tinker) {
            prefs.clearTinker(device.getID());
            for (Pin pin : allPins) {
                pin.setConfiguredAction(PinAction.NONE);
                pin.reset();
            }
            return true;

        } else if (DeviceMenuUrlHandler.handleActionItem(getActivity(), actionId, item.getTitle())) {
            return true;

        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void updateTitle() {
        String name = truthy(device.getName()) ? device.getName() : "(Unnamed device)";
        getActivity().setTitle(name);
    }

    private TextView findPinView(int id) {
        return Ui.findView(this, id);
    }

    private void loadViews() {
        // This is kind of wrong, since the other enum value for this type is "NONE", which isn't
        // a function, but it seemed even more absurd to create a whole second enum where 3/4 of
        // the values are identical
        EnumSet<PinAction> allFunctions = EnumSet.of(
                PinAction.ANALOG_READ,
                PinAction.ANALOG_WRITE,
                PinAction.DIGITAL_READ,
                PinAction.DIGITAL_WRITE);

        EnumSet<PinAction> noAnalogWrite = EnumSet.of(
                PinAction.ANALOG_READ,
                PinAction.DIGITAL_READ,
                PinAction.DIGITAL_WRITE);

        EnumSet<PinAction> noAnalogRead = EnumSet.of(
                PinAction.ANALOG_WRITE,
                PinAction.DIGITAL_READ,
                PinAction.DIGITAL_WRITE);

        EnumSet<PinAction> digitalOnly = EnumSet.of(
                PinAction.DIGITAL_READ,
                PinAction.DIGITAL_WRITE);

        switch (device.getDeviceType()) {
            case CORE:
                allPins.add(new Pin(findPinView(R.id.tinker_a0), PinType.A, "A0", allFunctions));
                allPins.add(new Pin(findPinView(R.id.tinker_a1), PinType.A, "A1", allFunctions));
                allPins.add(new Pin(findPinView(R.id.tinker_a2), PinType.A, "A2", noAnalogWrite));
                allPins.add(new Pin(findPinView(R.id.tinker_a3), PinType.A, "A3", noAnalogWrite));
                allPins.add(new Pin(findPinView(R.id.tinker_a4), PinType.A, "A4", allFunctions));
                allPins.add(new Pin(findPinView(R.id.tinker_a5), PinType.A, "A5", allFunctions));
                allPins.add(new Pin(findPinView(R.id.tinker_a6), PinType.A, "A6", allFunctions));
                allPins.add(new Pin(findPinView(R.id.tinker_a7), PinType.A, "A7", allFunctions));

                allPins.add(new Pin(findPinView(R.id.tinker_d0), PinType.D, "D0", noAnalogRead));
                allPins.add(new Pin(findPinView(R.id.tinker_d1), PinType.D, "D1", noAnalogRead));
                allPins.add(new Pin(findPinView(R.id.tinker_d2), PinType.D, "D2", digitalOnly));
                allPins.add(new Pin(findPinView(R.id.tinker_d3), PinType.D, "D3", digitalOnly));
                allPins.add(new Pin(findPinView(R.id.tinker_d4), PinType.D, "D4", digitalOnly));
                allPins.add(new Pin(findPinView(R.id.tinker_d5), PinType.D, "D5", digitalOnly));
                allPins.add(new Pin(findPinView(R.id.tinker_d6), PinType.D, "D6", digitalOnly));
                allPins.add(new Pin(findPinView(R.id.tinker_d7), PinType.D, "D7", digitalOnly));
                break;

            // default: Photon/"cuppa"
            case PHOTON:
            default:
                EnumSet<PinAction> allFunctionsDAC = EnumSet.of(
                        PinAction.ANALOG_READ,
                        PinAction.ANALOG_WRITE_DAC,
                        PinAction.DIGITAL_READ,
                        PinAction.DIGITAL_WRITE);

                allPins.add(new Pin(findPinView(R.id.tinker_a0), PinType.A, "A0", noAnalogWrite));
                allPins.add(new Pin(findPinView(R.id.tinker_a1), PinType.A, "A1", noAnalogWrite));
                allPins.add(new Pin(findPinView(R.id.tinker_a2), PinType.A, "A2", noAnalogWrite));
                allPins.add(new Pin(findPinView(R.id.tinker_a3), PinType.A, "A3", allFunctionsDAC, "A3", ANALOG_WRITE_MAX_ALT));
                // (II) Analog write duplicated to value in D3 (mention in UI)
                allPins.add(new Pin(findPinView(R.id.tinker_a4), PinType.A, "A4", allFunctions));
                // (I) Analog write duplicated to value in D2 (mention in UI)
                allPins.add(new Pin(findPinView(R.id.tinker_a5), PinType.A, "A5", allFunctions));
                allPins.add(new Pin(findPinView(R.id.tinker_a6), PinType.A, "A6", allFunctionsDAC, "DAC", ANALOG_WRITE_MAX_ALT));
                allPins.add(new Pin(findPinView(R.id.tinker_a7), PinType.A, "A7", allFunctions, "WKP", ANALOG_WRITE_MAX));

                allPins.add(new Pin(findPinView(R.id.tinker_d0), PinType.D, "D0", noAnalogRead));
                allPins.add(new Pin(findPinView(R.id.tinker_d1), PinType.D, "D1", noAnalogRead));
                allPins.add(new Pin(findPinView(R.id.tinker_d2), PinType.D, "D2", noAnalogRead));
                // (II) Analog write duplicated to value in A3 (mention in UI)
                allPins.add(new Pin(findPinView(R.id.tinker_d3), PinType.D, "D3", noAnalogRead));
                // (II) Analog write duplicated to value in A4 (mention in UI)
                allPins.add(new Pin(findPinView(R.id.tinker_d4), PinType.D, "D4", digitalOnly));
                allPins.add(new Pin(findPinView(R.id.tinker_d5), PinType.D, "D5", digitalOnly));
                allPins.add(new Pin(findPinView(R.id.tinker_d6), PinType.D, "D6", digitalOnly));
                allPins.add(new Pin(findPinView(R.id.tinker_d7), PinType.D, "D7", digitalOnly));
                break;
        }

        for (Pin pin : allPins) {
            pinsByName.put(pin.name, pin);
            PinAction pinFunction = prefs.getPinFunction(device.getID(), pin.name);
            pin.setConfiguredAction(pinFunction);
        }
    }

    private void setupListeners() {
        // Set up pin listeners
        for (final Pin pin : allPins) {
            for (View view : list(pin.view, (ViewGroup) pin.view.getParent())) {
                view.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Pin writeModePin = getPinInWriteMode();
                        if (writeModePin != null && !pin.equals(selectedPin)) {
                            writeModePin.showAnalogWriteValue();
                            unmutePins();
                            return;
                        }
                        selectedPin = pin;
                        onPinClick(pin);
                    }
                });

                view.setOnLongClickListener(new OnLongClickListener() {

                    @Override
                    public boolean onLongClick(View v) {
                        Pin writeModePin = getPinInWriteMode();
                        if (writeModePin != null && !pin.equals(selectedPin)) {
                            writeModePin.showAnalogWriteValue();
                            unmutePins();
                            return true;
                        }
                        selectedPin = pin;
                        showTinkerSelect(pin);
                        return true;
                    }
                });
            }
        }

        // Set up other listeners
        Ui.findView(this, R.id.tinker_main).setOnClickListener(this);
    }

    private void onPinClick(Pin selectedPin) {
        if (selectedPin.getConfiguredAction() != PinAction.NONE) {
            // Perform requested action
            switch (selectedPin.getConfiguredAction()) {
                case ANALOG_READ:
                    doAnalogRead(selectedPin);
                    break;
                case ANALOG_WRITE:
                    if (selectedPin.isAnalogWriteMode()) {
                        selectedPin.showAnalogWriteValue();
                        unmutePins();
                    } else {
                        doAnalogWrite(selectedPin);
                    }
                    break;
                case DIGITAL_READ:
                    doDigitalRead(selectedPin);
                    break;
                case DIGITAL_WRITE:
                    doDigitalWrite(selectedPin);
                    break;
                default:
                    break;
            }
        } else {
            showTinkerSelect(selectedPin);
        }
    }

    private void showTinkerSelect(Pin pin) {
        // No current action on the pin
        mutePinsExcept(pin);
        toggleViewVisibilityWithFade(R.id.tinker_logo, false);

        final View selectDialogView = getActivity().getLayoutInflater().inflate(
                R.layout.tinker_select, null);

        selectDialog = new AlertDialog.Builder(getActivity(),
                R.style.ParticleSetupTheme_DialogNoDimBackground)
                .setView(selectDialogView)
                .setCancelable(true)
                .setOnCancelListener(new OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                    }
                })
                .create();
        selectDialog.setCanceledOnTouchOutside(true);
        selectDialog.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                unmutePins();
                toggleViewVisibilityWithFade(R.id.tinker_logo, true);
                selectDialog = null;
            }
        });

        final View analogRead = Ui.findView(selectDialogView, R.id.tinker_button_analog_read);
        final View analogWrite = Ui.findView(selectDialogView, R.id.tinker_button_analog_write);
        final View digitalRead = Ui.findView(selectDialogView, R.id.tinker_button_digital_read);
        final View digitalWrite = Ui.findView(selectDialogView, R.id.tinker_button_digital_write);
        final List<View> allButtons = list(analogRead, analogWrite, digitalRead, digitalWrite);

        analogRead.setOnTouchListener(new OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    setTinkerSelectButtonSelected(analogRead, allButtons);
                }
                return false;
            }
        });

        analogWrite.setOnTouchListener(new OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    setTinkerSelectButtonSelected(analogWrite, allButtons);
                }
                return false;
            }
        });

        digitalRead.setOnTouchListener(new OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    setTinkerSelectButtonSelected(digitalRead, allButtons);
                }
                return false;
            }
        });

        digitalWrite.setOnTouchListener(new OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    setTinkerSelectButtonSelected(digitalWrite, allButtons);
                }
                return false;
            }
        });

        digitalRead.setOnClickListener(this);
        digitalWrite.setOnClickListener(this);
        analogRead.setOnClickListener(this);
        analogWrite.setOnClickListener(this);

        setVisible(digitalRead, pin.getFunctions().contains(PinAction.DIGITAL_READ));
        setVisible(digitalWrite, pin.getFunctions().contains(PinAction.DIGITAL_WRITE));
        setVisible(analogRead, pin.getFunctions().contains(PinAction.ANALOG_READ));
        setVisible(analogWrite, (pin.getFunctions().contains(PinAction.ANALOG_WRITE)
                || pin.getFunctions().contains(PinAction.ANALOG_WRITE_DAC)));

        ((TextView) selectDialogView.findViewById(R.id.tinker_select_pin)).setText(pin.label);

        PinAction action = pin.getConfiguredAction();
        switch (action) {
            case ANALOG_READ:
                setTinkerSelectButtonSelected(analogRead, allButtons);
                break;

            case ANALOG_WRITE_DAC:
            case ANALOG_WRITE:
                setTinkerSelectButtonSelected(analogWrite, allButtons);
                break;

            case DIGITAL_READ:
                setTinkerSelectButtonSelected(digitalRead, allButtons);
                break;

            case DIGITAL_WRITE:
                setTinkerSelectButtonSelected(digitalWrite, allButtons);
                break;

            case NONE:
                setTinkerSelectButtonSelected(null, allButtons);
                break;
        }

        selectDialog.show();

        View decorView = selectDialog.getWindow().getDecorView();
        noIReallyMeanItIWantThisToBeTransparent(decorView);
    }

    private void setVisible(View view, boolean shouldBeVisible) {
        view.setVisibility(shouldBeVisible ? View.VISIBLE : View.INVISIBLE);
    }

    private void setTinkerSelectButtonSelected(View selectButtonView, List<View> allButtons) {
        for (View button : allButtons) {
            Ui.findView(button, R.id.tinker_button_color)
                    .setVisibility((button == selectButtonView) ? View.VISIBLE : View.INVISIBLE);
            button.setBackgroundResource(
                    (button == selectButtonView)
                            ? R.color.tinker_selection_overlay_item_selected_bg
                            : R.color.tinker_selection_overlay_item_bg);
        }
    }

    private void noIReallyMeanItIWantThisToBeTransparent(View view) {
        if (view.getId() == R.id.tinker_select) {
            return;
        }
        view.setBackgroundColor(0);
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                noIReallyMeanItIWantThisToBeTransparent(vg.getChildAt(i));
            }
        }
    }

    private void toggleViewVisibilityWithFade(int viewId, final boolean show) {
        final View view = Ui.findView(this, viewId);
        int shortAnimTime = 150; // ms
        view.setVisibility(View.VISIBLE);
        view.animate()
                .setDuration(shortAnimTime)
                .alpha(show ? 1 : 0)
                .setListener(new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(show ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private void mutePinsExcept(Pin pin) {
        for (Pin currentPin : allPins) {
            if (!currentPin.equals(pin)) {
                currentPin.mute();
            }
        }
    }

    private void unmutePins() {
        // Unmute pins
        for (Pin pin : allPins) {
            pin.unmute();
        }
    }

    private void hideTinkerSelect() {
        // Reset tinker select dialog state
        toggleViewVisibilityWithFade(R.id.tinker_logo, true);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tinker_button_analog_read:
                onFunctionSelected(selectedPin, PinAction.ANALOG_READ);
                break;
            case R.id.tinker_button_analog_write:
                onFunctionSelected(selectedPin, PinAction.ANALOG_WRITE);
                break;
            case R.id.tinker_button_digital_read:
                onFunctionSelected(selectedPin, PinAction.DIGITAL_READ);
                break;
            case R.id.tinker_button_digital_write:
                onFunctionSelected(selectedPin, PinAction.DIGITAL_WRITE);
                break;
            case R.id.tinker_main:
                for (Pin pin : allPins) {
                    if (pin.isAnalogWriteMode()) {
                        pin.showAnalogWriteValue();
                    }
                }
                unmutePins();
                // hideTinkerSelect();
                break;
        }
    }

    private Pin getPinInWriteMode() {
        for (Pin pin : allPins) {
            if (pin.isAnalogWriteMode()) {
                return pin;
            }
        }
        return null;
    }

    private void onFunctionSelected(Pin selectedPin, PinAction action) {
        if (selectDialog != null) {
            selectDialog.dismiss();
            selectDialog = null;
        }
        toggleViewVisibilityWithFade(R.id.tinker_logo, true);

        selectedPin.reset();
        selectedPin.setConfiguredAction(action);
        prefs.savePinFunction(device.getID(), selectedPin.name, action);
        // FIXME: should this actually be commented out?
//		 hideTinkerSelect();
//		 unmutePins();
    }

    private void doAnalogRead(Pin pin) {
        pin.animateYourself();
        api.read(new PinStuff(pin.name, PinAction.ANALOG_READ, pin.getAnalogValue()));
        // pin.showAnalogRead(850);
    }

    private void doAnalogWrite(final Pin pin) {
        mutePinsExcept(pin);
        toggleViewVisibilityWithFade(R.id.tinker_logo, false);
        pin.showAnalogWrite(new OnAnalogWriteListener() {

            @Override
            public void onAnalogWrite(int value) {
                for (Pin pin : allPins) {
                    if (pin.isAnalogWriteMode()) {
                        pin.showAnalogWriteValue();
                    }
                }
                unmutePins();
                hideTinkerSelect();
                pin.animateYourself();
                api.write(new PinStuff(pin.name, PinAction.ANALOG_WRITE, pin.getAnalogValue()), value);
            }
        });
    }

    private void doDigitalRead(Pin pin) {
        pin.animateYourself();
        api.read(new PinStuff(pin.name, PinAction.DIGITAL_READ, pin.getDigitalValue().asInt()));
        // pin.showDigitalRead(DigitalValue.HIGH);
    }

    private void doDigitalWrite(Pin pin) {
        pin.animateYourself();
        DigitalValue currentValue = pin.getDigitalValue();
        DigitalValue newValue = (currentValue == DigitalValue.HIGH)
                ? DigitalValue.LOW
                : DigitalValue.HIGH;
        api.write(new PinStuff(pin.name, PinAction.DIGITAL_WRITE, currentValue.asInt()), newValue.asInt());
        // pin.showDigitalWrite(newValue);
    }

    private void onTinkerCallComplete(PinStuff stuff, int valueToApply) {
        log.d("onTinkerCallComplete()");

        Pin pin = pinsByName.get(stuff.pinName);

        if (pin.getConfiguredAction() == PinAction.NONE) {
            // received a response for a pin that has since been cleared
            pin.stopAnimating();
            return;
        }

        if (stuff.pinAction == PinAction.ANALOG_READ || stuff.pinAction == PinAction.ANALOG_WRITE) {
            pin.showAnalogValue(valueToApply);
        } else {
            pin.showDigitalRead(DigitalValue.fromInt(valueToApply));
        }
    }


    private abstract class TinkerWork extends Async.ApiWork<ParticleDevice, Integer> {

        final PinStuff stuff;

        protected TinkerWork(PinStuff stuff) {
            this.stuff = stuff;
        }

        @Override
        public void onFailure(ParticleCloudException exception) {
            onTinkerCallComplete(stuff, stuff.currentValue);
            // FIXME: do real error handling!
//			ErrorsDelegate errorsDelegate = ((BaseActivity) getActivity()).getErrorsDelegate();
//			errorsDelegate.showTinkerError();
        }

    }


    private class TinkerApi {

        private final Map<PinAction, String> actionToFunctionName;

        TinkerApi() {
            actionToFunctionName = new ArrayMap<>(4);
            actionToFunctionName.put(PinAction.ANALOG_READ, "analogread");
            actionToFunctionName.put(PinAction.ANALOG_WRITE, "analogwrite");
            actionToFunctionName.put(PinAction.DIGITAL_READ, "digitalread");
            actionToFunctionName.put(PinAction.DIGITAL_WRITE, "digitalwrite");
        }

        void write(final PinStuff stuff, final int newValue) {
            Async.executeAsync(device, new TinkerWork(stuff) {
                @Override
                public Integer callApi(ParticleDevice sparkDevice) throws ParticleCloudException, IOException {
                    String stringValue;
                    if (stuff.pinAction == PinAction.ANALOG_WRITE) {
                        stringValue = String.valueOf(newValue);
                    } else {
                        stringValue = (newValue == DigitalValue.HIGH.asInt()) ? "HIGH" : "LOW";
                    }
                    try {
                        return (sparkDevice.callFunction(
                                actionToFunctionName.get(stuff.pinAction),
                                list(stuff.pinName, stringValue))==1) ? newValue : stuff.currentValue;
                    } catch (final ParticleDevice.FunctionDoesNotExistException e) {
                        Toaster.s(getActivity(), e.getMessage());
                        return stuff.currentValue; // it didn't change
                    }

                }

                @Override
                public void onSuccess(Integer returnValue) {
                    onTinkerCallComplete(stuff, returnValue);
                }
            });
        }

        void read(PinStuff stuff) {
            Async.executeAsync(device, new TinkerWork(stuff) {
                @Override
                public Integer callApi(ParticleDevice sparkDevice) throws ParticleCloudException,
                        IOException {
                    try {
                        return sparkDevice.callFunction(
                                actionToFunctionName.get(stuff.pinAction),
                                list(stuff.pinName));
                    } catch (ParticleDevice.FunctionDoesNotExistException e) {
                        Toaster.s(getActivity(), e.getMessage());
                        return stuff.currentValue;
                    }
                }

                @Override
                public void onSuccess(Integer returnValue) {
                    onTinkerCallComplete(stuff, returnValue);
                }
            });
        }
    }


    private class DevicesUpdatedListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateTitle();
        }

        IntentFilter buildIntentFilter() {
            return new IntentFilter(BroadcastContract.BROADCAST_DEVICES_UPDATED);
        }
    }


    // FIXME: rename to something more descriptive
    static class PinStuff {

        final String pinName;
        final PinAction pinAction;
        final int currentValue;

        PinStuff(String pinName, PinAction pinAction, int currentValue) {
            this.pinName = pinName;
            this.pinAction = pinAction;
            this.currentValue = currentValue;
        }

        @Override
        public String toString() {
            return "PinStuff{" +
                    "pinName='" + pinName + '\'' +
                    ", pinAction=" + pinAction +
                    ", currentValue=" + currentValue +
                    '}';
        }
    }


    // Doing this as a fragment because I ran into touch issues doing it as just a view,
    // and because this gives us back button support at no additional charge.
    public static class InstructionsFragment extends Fragment {

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.tinker_instructions, container, false);
            v.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    TinkerPrefs.getInstance(getActivity()).setVisited(true);
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });

            return v;
        }
    }


    static class DeviceMenuUrlHandler {

        private static final SparseIntArray menuIdsToUris = new SparseIntArray();

        static {
            menuIdsToUris.put(R.id.action_show_docs_particle_app_tinker, R.string.uri_docs_particle_app_tinker);
            menuIdsToUris.put(R.id.action_show_docs_setting_up_your_device, R.string.uri_docs_setting_up_your_device);
            menuIdsToUris.put(R.id.action_show_docs_create_your_own_android_app, R.string.uri_docs_create_your_own_android_app);
            menuIdsToUris.put(R.id.action_support_show_community, R.string.uri_support_community);
            menuIdsToUris.put(R.id.action_support_show_support_site, R.string.uri_support_site);
        }

        /**
         * Attempt to handle the action item with the given ID.
         *
         * @return true if action item was handled, else false.
         */
        public static boolean handleActionItem(Activity activity, int actionItemId, CharSequence titleToShow) {
            if (menuIdsToUris.indexOfKey(actionItemId) < 0) {
                // we don't handle this ID.
                return false;
            }
            Uri uri = Uri.parse(activity.getString(menuIdsToUris.get(actionItemId)));
            activity.startActivity(WebViewActivity.buildIntent(activity, uri, titleToShow));
            return true;
        }

    }

}

