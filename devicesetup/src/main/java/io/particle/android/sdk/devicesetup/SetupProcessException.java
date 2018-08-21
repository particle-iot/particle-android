package io.particle.android.sdk.devicesetup;


import io.particle.android.sdk.devicesetup.setupsteps.SetupStep;

public class SetupProcessException extends Exception {

    public final SetupStep failedStep;

    public SetupProcessException(String msg, SetupStep failedStep) {
        super(msg);
        this.failedStep = failedStep;
    }
}
