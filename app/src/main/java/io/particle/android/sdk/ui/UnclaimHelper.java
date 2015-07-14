package io.particle.android.sdk.ui;


import android.app.Activity;
import android.support.v4.app.FragmentActivity;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;

import java.io.IOException;

import io.particle.android.sdk.cloud.SparkCloudException;
import io.particle.android.sdk.cloud.SparkDevice;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.ui.Toaster;
import io.particle.sdk.app.R;


public class UnclaimHelper {

    public static void unclaimDeviceWithDialog(final FragmentActivity activity, final SparkDevice device) {
        new MaterialDialog.Builder(activity)
                .content(R.string.unclaim_device_dialog_content)
                .theme(Theme.LIGHT)
                .autoDismiss(true)
                .positiveText(R.string.unclaim)
                .negativeText(R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        unclaim(activity, device);
                    }
                })
                .show();
    }

    private static void unclaim(final Activity activity, final SparkDevice device) {
        Async.executeAsync(device, new Async.ApiWork<SparkDevice, Void>() {

            @Override
            public Void callApi(SparkDevice sparkDevice) throws SparkCloudException, IOException {
                device.unclaim();
                return null;
            }

            @Override
            public void onSuccess(Void aVoid) {
                // FIXME: what else should happen here?
                Toaster.s(activity, "Unclaimed " + device.getName());
            }

            @Override
            public void onFailure(SparkCloudException exception) {
                new MaterialDialog.Builder(activity)
                        .content("Error: unable to unclaim '" + device.getName() + "'")
                        .theme(Theme.LIGHT)
                        .autoDismiss(true)
                        .positiveText("OK")
                        .show();
            }
        });
    }
}
