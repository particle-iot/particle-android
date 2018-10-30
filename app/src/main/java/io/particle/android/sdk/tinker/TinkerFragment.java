package io.particle.android.sdk.tinker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.collection.ArrayMap;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.f2prateek.bundler.FragmentBundlerCompat;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import io.particle.android.sdk.cloud.BroadcastContract;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException;
import io.particle.android.sdk.ui.DeviceActionsHelper;
import io.particle.android.sdk.ui.DeviceMenuUrlHandler;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Prefs;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.Toaster;
import io.particle.android.sdk.utils.ui.Ui;
import io.particle.sdk.app.R;

import static io.particle.android.sdk.utils.Py.list;
import static io.particle.android.sdk.utils.Py.map;


/**
 * A fragment representing a single Tinker screen.
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tinker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_DEVICE, device);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // we handle both the context device row actions here and our own
//        inflater.inflate(R.menu.context_device_row, menu);
        inflater.inflate(R.menu.tinker, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int actionId = item.getItemId();
        if (DeviceActionsHelper.takeActionForDevice(actionId, getActivity(), device)) {
            return true;

        } else if (actionId == R.id.action_device_clear_tinker) {
            prefs.clearTinker(device.getId());
            for (Pin pin : allPins) {
                pin.setConfiguredAction(PinAction.NONE);
                pin.reset();
            }
            return true;

        } else {
            return DeviceMenuUrlHandler.handleActionItem(getActivity(), actionId, item.getTitle()) ||
                    super.onOptionsItemSelected(item);
        }
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
            PinAction pinFunction = prefs.getPinFunction(device.getId(), pin.name);
            pin.setConfiguredAction(pinFunction);
        }
    }

    private void setupListeners() {
        // Set up pin listeners
        for (final Pin pin : allPins) {
            for (View view : list(pin.view, (ViewGroup) pin.view.getParent())) {
                view.setOnClickListener(v -> {
                    Pin writeModePin = getPinInWriteMode();
                    if (writeModePin != null && !pin.equals(selectedPin)) {
                        writeModePin.showAnalogWriteValue();
                        unmutePins();
                        return;
                    }
                    selectedPin = pin;
                    onPinClick(pin);
                });

                view.setOnLongClickListener(v -> {
                    Pin writeModePin = getPinInWriteMode();
                    if (writeModePin != null && !pin.equals(selectedPin)) {
                        writeModePin.showAnalogWriteValue();
                        unmutePins();
                        return true;
                    }
                    selectedPin = pin;
                    showTinkerSelect(pin);
                    return true;
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
                R.layout.tinker_select, (ViewGroup) getView(), false);

        selectDialog = new AlertDialog.Builder(getActivity(),
                R.style.ParticleSetupTheme_DialogNoDimBackground)
                .setView(selectDialogView)
                .setCancelable(true)
                .setOnCancelListener(DialogInterface::dismiss)
                .create();
        selectDialog.setCanceledOnTouchOutside(true);
        selectDialog.setOnDismissListener(dialog -> {
            unmutePins();
            toggleViewVisibilityWithFade(R.id.tinker_logo, true);
            selectDialog = null;
        });

        final View analogRead = Ui.findView(selectDialogView, R.id.tinker_button_analog_read);
        final View analogWrite = Ui.findView(selectDialogView, R.id.tinker_button_analog_write);
        final View digitalRead = Ui.findView(selectDialogView, R.id.tinker_button_digital_read);
        final View digitalWrite = Ui.findView(selectDialogView, R.id.tinker_button_digital_write);
        final List<View> allButtons = list(analogRead, analogWrite, digitalRead, digitalWrite);

        analogRead.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                setTinkerSelectButtonSelected(analogRead, allButtons);
            }
            return false;
        });

        analogWrite.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                setTinkerSelectButtonSelected(analogWrite, allButtons);
            }
            return false;
        });

        digitalRead.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                setTinkerSelectButtonSelected(digitalRead, allButtons);
            }
            return false;
        });

        digitalWrite.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                setTinkerSelectButtonSelected(digitalWrite, allButtons);
            }
            return false;
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
        prefs.savePinFunction(device.getId(), selectedPin.name, action);
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
        pin.showAnalogWrite(value -> {
            for (Pin pin1 : allPins) {
                if (pin1.isAnalogWriteMode()) {
                    pin1.showAnalogWriteValue();
                }
            }
            unmutePins();
            hideTinkerSelect();
            pin.animateYourself();
            pin.showAnalogValue(value);
            api.write(new PinStuff(pin.name, PinAction.ANALOG_WRITE, pin.getAnalogValue()), value);
        });
    }

    private void doDigitalRead(Pin pin) {
        pin.animateYourself();
        api.read(new PinStuff(pin.name, PinAction.DIGITAL_READ, pin.getDigitalValue().getIntValue()));
        // pin.showDigitalRead(DigitalValue.HIGH);
    }

    private void doDigitalWrite(Pin pin) {
        pin.animateYourself();
        DigitalValue currentValue = pin.getDigitalValue();
        DigitalValue newValue = (currentValue == DigitalValue.HIGH)
                ? DigitalValue.LOW
                : DigitalValue.HIGH;
        api.write(new PinStuff(pin.name, PinAction.DIGITAL_WRITE, currentValue.getIntValue()), newValue.getIntValue());
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

        TinkerWork(PinStuff stuff) {
            this.stuff = stuff;
        }

        @Override
        public void onFailure(@NonNull ParticleCloudException exception) {
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
            try {
                Async.executeAsync(device, new TinkerWork(stuff) {
                    @Override
                    public Integer callApi(@NonNull ParticleDevice sparkDevice) throws ParticleCloudException, IOException {
                        String stringValue;
                        if (stuff.pinAction == PinAction.ANALOG_WRITE) {
                            stringValue = String.valueOf(newValue);
                        } else {
                            stringValue = (newValue == DigitalValue.HIGH.getIntValue()) ? "HIGH" : "LOW";
                        }
                        try {
                            return (sparkDevice.callFunction(
                                    actionToFunctionName.get(stuff.pinAction),
                                    list(stuff.pinName, stringValue)) == 1) ? newValue : stuff.currentValue;
                        } catch (final ParticleDevice.FunctionDoesNotExistException e) {
                            Toaster.s(getActivity(), e.getMessage());
                            return stuff.currentValue; // it didn't change
                        }
                    }

                    @Override
                    public void onSuccess(@NonNull Integer returnValue) {
                        onTinkerCallComplete(stuff, returnValue);
                    }
                });
            } catch (ParticleCloudException e) {
                // should we show a message here? ignore it all together?
            }
        }

        void read(PinStuff stuff) {
            try {
                Async.executeAsync(device, new TinkerWork(stuff) {
                    @Override
                    public Integer callApi(@NonNull ParticleDevice sparkDevice) throws ParticleCloudException, IOException {
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
                    public void onSuccess(@NonNull Integer returnValue) {
                        onTinkerCallComplete(stuff, returnValue);
                    }
                });
            } catch (ParticleCloudException e) {
                // should we show a message here? ignore it all together?
            }
        }
    }


    private class DevicesUpdatedListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
        }

        IntentFilter buildIntentFilter() {
            return new IntentFilter(BroadcastContract.BROADCAST_DEVICES_UPDATED);
        }
    }


    // FIXME: rename to something more descriptive
    private static class PinStuff {

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
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.tinker_instructions, container, false);
            v.setOnClickListener(v1 -> {
                TinkerPrefs.getInstance(getActivity()).setVisited(true);
                getActivity().getSupportFragmentManager().popBackStack();
            });

            return v;
        }
    }

}

