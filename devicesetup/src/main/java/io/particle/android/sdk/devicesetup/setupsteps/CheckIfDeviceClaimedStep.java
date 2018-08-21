package io.particle.android.sdk.devicesetup.setupsteps;


import java.util.List;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleDevice;


public class CheckIfDeviceClaimedStep extends SetupStep {

    private final ParticleCloud sparkCloud;
    private final String deviceBeingConfiguredId;
    private boolean needToClaimDevice = true;

    CheckIfDeviceClaimedStep(StepConfig stepConfig, ParticleCloud sparkCloud,
                                    String deviceBeingConfiguredId) {
        super(stepConfig);
        this.sparkCloud = sparkCloud;
        this.deviceBeingConfiguredId = deviceBeingConfiguredId;
    }

    @Override
    protected void onRunStep() throws SetupStepException {
        List<ParticleDevice> devices;
        try {
            devices = sparkCloud.getDevices();
        } catch (ParticleCloudException e) {
            throw new SetupStepException(e);
        }

        log.d("Got devices back from the cloud...");
        for (ParticleDevice device : devices) {
            if (deviceBeingConfiguredId.equalsIgnoreCase(device.getID())) {
                log.d("Success, device " + device.getID() + " claimed!");
                needToClaimDevice = false;
                return;
            }
        }

        // device not found in the loop
        throw new SetupStepException("Device " + deviceBeingConfiguredId + " still not claimed.");
    }

    @Override
    public boolean isStepFulfilled() {
        return !needToClaimDevice;
    }

}
