package io.particle.android.sdk.devicesetup.ui;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.SetupProcessException;
import io.particle.android.sdk.devicesetup.setupsteps.SetupStep;
import io.particle.android.sdk.devicesetup.setupsteps.SetupStepsRunnerTask;
import io.particle.android.sdk.devicesetup.setupsteps.StepProgress;
import io.particle.android.sdk.utils.CoreNameGenerator;
import io.particle.android.sdk.utils.EZ;
import io.particle.android.sdk.utils.Funcy;
import io.particle.android.sdk.utils.Py;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.ui.Ui;

import static io.particle.android.sdk.utils.Py.set;
import static io.particle.android.sdk.utils.Py.truthy;

/**
 * Created by Julius.
 */

public class ConnectingProcessWorkerTask extends SetupStepsRunnerTask {
    private static final TLog log = TLog.get(ConnectingProcessWorkerTask.class);

    @Inject protected ParticleCloud sparkCloud;

    private WeakReference<Activity> activityReference;
    private String deviceId;
    private Drawable tintedSpinner;
    private Drawable tintedCheckmark;

    ConnectingProcessWorkerTask(Activity activity, List<SetupStep> steps, int maxOverallAttempts) {
        super(steps, maxOverallAttempts);
        ParticleDeviceSetupLibrary.getInstance().getApplicationComponent().activityComponentBuilder()
                .build().inject(this);
        this.deviceId = DeviceSetupState.deviceToBeSetUpId;
        this.activityReference = new WeakReference<>(activity);
        this.tintedSpinner = Ui.getTintedDrawable(activity, R.drawable.progress_spinner, R.color.element_tint_color);
        this.tintedCheckmark = Ui.getTintedDrawable(activity, R.drawable.checkmark, R.color.element_tint_color);
    }

    @Override
    protected void onProgressUpdate(StepProgress... values) {
        Activity activity = activityReference.get();

        if (activity == null) {
            return;
        }

        for (StepProgress progress : values) {
            View v = activity.findViewById(progress.stepId);
            if (v != null) {
                updateProgress(progress, v);
            }
        }
    }

    @Override
    protected void onPostExecute(SetupProcessException error) {
        int resultCode;

        if (error != null) {
            resultCode = error.failedStep.getStepConfig().resultCode;

        } else {
            log.d("HUZZAH, VICTORY!");
            // FIXME: handle "success, no ownership" case
            resultCode = SuccessActivity.RESULT_SUCCESS;

            EZ.runAsync(() -> {
                try {
                    // collect a list of unique, non-null device names
                    Set<String> names = set(Funcy.transformList(
                            sparkCloud.getDevices(),
                            Funcy.notNull(),
                            ParticleDevice::getName,
                            Py::truthy
                    ));
                    ParticleDevice device = sparkCloud.getDevice(deviceId);
                    if (device != null && !truthy(device.getName())) {
                        device.setName(CoreNameGenerator.generateUniqueName(names));
                    }
                } catch (Exception e) {
                    // FIXME: do real error handling here, and only
                    // handle ParticleCloudException instead of swallowing everything
                    e.printStackTrace();
                }
            });
        }

        Activity activity = activityReference.get();
        if (activity != null) {
            activity.startActivity(SuccessActivity.buildIntent(activity, resultCode, deviceId));
            activity.finish();
        }
    }

    private void updateProgress(StepProgress progress, View progressStepContainer) {
        ProgressBar progBar = Ui.findView(progressStepContainer, R.id.spinner);
        ImageView checkmark = Ui.findView(progressStepContainer, R.id.checkbox);

        // don't show the spinner again if we've already shown the checkmark,
        // regardless of the underlying state that might hide
        if (checkmark.getVisibility() == View.VISIBLE) {
            return;
        }

        progressStepContainer.setVisibility(View.VISIBLE);

        if (progress.status == StepProgress.STARTING) {
            checkmark.setVisibility(View.GONE);

            progBar.setProgressDrawable(tintedSpinner);
            progBar.setVisibility(View.VISIBLE);

        } else {
            progBar.setVisibility(View.GONE);

            checkmark.setImageDrawable(tintedCheckmark);
            checkmark.setVisibility(View.VISIBLE);
        }
    }
}