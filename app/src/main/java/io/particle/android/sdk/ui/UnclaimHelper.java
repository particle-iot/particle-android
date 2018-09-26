package io.particle.android.sdk.ui;


import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;

import java.io.IOException;

import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.ui.Toaster;
import io.particle.sdk.app.R;


class UnclaimHelper {

    static void unclaimDeviceWithDialog(final FragmentActivity activity, final ParticleDevice device) {
        new AlertDialog.Builder(activity)
                .setMessage(R.string.unclaim_device_dialog_content)
                .setPositiveButton(R.string.unclaim, (dialog, which) -> unclaim(activity, device))
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private static void unclaim(final Activity activity, final ParticleDevice device) {
        try {
            Async.executeAsync(device, new Async.ApiWork<ParticleDevice, Void>() {

                @Override
                public Void callApi(@NonNull ParticleDevice sparkDevice)
                        throws ParticleCloudException, IOException {
                    device.unclaim();
                    return null;
                }

                @Override
                public void onSuccess(@NonNull Void aVoid) {
                    // FIXME: what else should happen here?
                    Toaster.s(activity, "Unclaimed " + device.getName());
                }

                @Override
                public void onFailure(@NonNull ParticleCloudException exception) {
                    new AlertDialog.Builder(activity)
                            .setMessage("Error: unable to unclaim '" + device.getName() + "'")
                            .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                            .show();
                }
            }).andIgnoreCallbacksIfActivityIsFinishing(activity);
        } catch (ParticleCloudException e) {
            new AlertDialog.Builder(activity)
                    .setMessage("Error: unable to unclaim '" + device.getName() + "'")
                    .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }
}
