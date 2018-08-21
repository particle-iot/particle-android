package io.particle.android.sdk.devicesetup.setupsteps;

import android.os.AsyncTask;

import java.util.List;

import io.particle.android.sdk.devicesetup.SetupProcessException;
import io.particle.android.sdk.utils.EZ;
import io.particle.android.sdk.utils.TLog;


public abstract class SetupStepsRunnerTask extends
        AsyncTask<Void, StepProgress, SetupProcessException> {

    private final TLog log = TLog.get(getClass());

    private final List<SetupStep> steps;
    private final int maxOverallAttempts;

    public SetupStepsRunnerTask(List<SetupStep> steps, int maxOverallAttempts) {
        this.steps = steps;
        this.maxOverallAttempts = maxOverallAttempts;
    }

    @Override
    public SetupProcessException doInBackground(Void... voids) {
        int attempts = 0;
        // We should never hit this limit, but just in case, we want to
        // avoid an infinite loop
        while (attempts < maxOverallAttempts) {
            attempts++;
            try {
                runSteps();
                // we got all the way through the steps, break out of the loop!
                return null;

            } catch (SetupStepException e) {
                log.w("Setup step failed: " + e.getMessage());

            } catch (SetupProcessException e) {
                return e;
            }
        }

        return new SetupProcessException("(Unknown setup error)", null);
    }

    private void runSteps() throws SetupStepException, SetupProcessException {
        for (SetupStep step : steps) {

            throwIfCancelled();

            publishProgress(new StepProgress(
                    step.getStepConfig().getStepId(),
                    StepProgress.STARTING));

            try {
                EZ.threadSleep(1000);
                throwIfCancelled();

                step.runStep();

            } catch (SetupStepException e) {
                // give it a moment before trying again.
                EZ.threadSleep(2000);
                throw e;
            }

            publishProgress(new StepProgress(
                    step.getStepConfig().getStepId(),
                    StepProgress.SUCCEEDED));
        }
    }

    private void throwIfCancelled() {
        // FIXME: while it's good that we handle being cancelled, this doesn't seem like
        // an ideal way to do it...
        if (isCancelled()) {
            throw new RuntimeException("Task was cancelled");
        }
    }
}
