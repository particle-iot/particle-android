package io.particle.android.sdk.devicesetup.setupsteps;

import io.particle.android.sdk.devicesetup.SetupProcessException;
import io.particle.android.sdk.devicesetup.ui.DeviceSetupState;
import io.particle.android.sdk.utils.EZ;
import io.particle.android.sdk.utils.Preconditions;
import io.particle.android.sdk.utils.SSID;
import io.particle.android.sdk.utils.WifiFacade;


public class WaitForDisconnectionFromDeviceStep extends SetupStep {

    private final SSID softApName;
    private final WifiFacade wifiFacade;

    private boolean wasDisconnected = false;

    WaitForDisconnectionFromDeviceStep(StepConfig stepConfig, SSID softApSSID, WifiFacade wifiFacade) {
        super(stepConfig);
        Preconditions.checkNotNull(softApSSID, "softApSSID cannot be null.");
        this.softApName = softApSSID;
        this.wifiFacade = wifiFacade;
    }

    @Override
    public boolean isStepFulfilled() {
        return wasDisconnected;
    }

    @Override
    protected void onRunStep() throws SetupStepException, SetupProcessException {
        for (int i = 0; i <= 5; i++) {
            if (isConnectedToSoftAp()) {
                // wait and try again
                EZ.threadSleep(200);
            } else {
                EZ.threadSleep(1000);
                // success, no longer connected.
                wasDisconnected = true;
                if (EZ.isUsingOlderWifiStack()) {
                    // for some reason Lollipop doesn't need this??
                    reenablePreviousWifi();
                }
                return;
            }
        }

        // Still connected after the above completed: fail
        throw new SetupStepException("Not disconnected from soft AP");
    }

    private void reenablePreviousWifi() {
        SSID prevSSID = DeviceSetupState.previouslyConnectedWifiNetwork;
        wifiFacade.reenableNetwork(prevSSID);
        wifiFacade.reassociate();
    }

    private boolean isConnectedToSoftAp() {
        SSID currentlyConnectedSSID = wifiFacade.getCurrentlyConnectedSSID();
        log.d("Currently connected SSID: " + currentlyConnectedSSID);
        return softApName.equals(currentlyConnectedSSID);
    }

}
