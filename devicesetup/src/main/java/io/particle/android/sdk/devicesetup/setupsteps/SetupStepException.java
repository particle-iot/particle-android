package io.particle.android.sdk.devicesetup.setupsteps;


public class SetupStepException extends Exception {

    public SetupStepException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

    public SetupStepException(String msg) {
        super(msg);
    }

    public SetupStepException(Throwable throwable) {
        super(throwable);
    }

}
