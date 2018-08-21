package io.particle.android.sdk.devicesetup.setupsteps;

import android.annotation.SuppressLint;
import android.support.annotation.AnyThread;
import android.support.annotation.WorkerThread;

import io.particle.android.sdk.devicesetup.SetupProcessException;
import io.particle.android.sdk.utils.TLog;


@WorkerThread
public abstract class SetupStep {

    protected final TLog log;
    private final StepConfig stepConfig;
    private volatile int numAttempts;


    public SetupStep(StepConfig stepConfig) {
        log = TLog.get(this.getClass());
        this.stepConfig = stepConfig;
    }

    protected abstract void onRunStep() throws SetupStepException, SetupProcessException;

    public abstract boolean isStepFulfilled();

    final void runStep() throws SetupStepException, SetupProcessException {
        if (isStepFulfilled()) {
            getLog().i("Step " + getStepName() + " already fulfilled, skipping...");
            return;
        }
        if (numAttempts > stepConfig.maxAttempts) {
            @SuppressLint("DefaultLocale")
            String msg = String.format("Exceeded limit of %d retries for step %s",
                    stepConfig.maxAttempts, getStepName());
            throw new SetupProcessException(msg, this);
        } else {
            getLog().i("Running step " + getStepName());
            numAttempts++;
            onRunStep();
        }
    }

    public TLog getLog() {
        return log;
    }

    @AnyThread
    public StepConfig getStepConfig() {
        return this.stepConfig;
    }

    protected void resetAttemptsCount() {
        numAttempts = 0;
    }

    private String getStepName() {
        return this.getClass().getSimpleName();
    }

}
