package io.particle.android.sdk.ui;


import android.support.v4.app.FragmentActivity;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;

import java.io.IOException;
import java.util.Set;

import io.particle.android.sdk.cloud.SparkCloudException;
import io.particle.android.sdk.cloud.SparkDevice;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.CoreNameGenerator;


public class RenameHelper {

    public static void renameDevice(FragmentActivity activity, SparkDevice device) {
        new RenameHelper(device, activity).showDialog();
    }


    private final SparkDevice device;
    private final FragmentActivity activity;

    private RenameHelper(SparkDevice device, FragmentActivity activity) {
        this.device = device;
        this.activity = activity;
    }


    private void showDialog() {
        // FIXME: include "suggest different name" button
        final String suggestedName = CoreNameGenerator.generateUniqueName(
                device.getCloud().getDeviceNames());

        new MaterialDialog.Builder(activity)
                .title("Rename Device")
                .content("Set new name for your device:")
                .theme(Theme.LIGHT)
                .input("Name your device", suggestedName, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        // FIXME: do validation here to prevent short names?
                        // I think this is already done cloud-side.  Verify.
                    }
                })
                .positiveText("Rename")
                .negativeText("Cancel")
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        Runnable onDupeName = new Runnable() {
                            @Override
                            public void run() {
                                showDialog();
                            }
                        };

                        // use one-method-per-line style here on the off chance that
                        // something really could be null here (it shouldn't be
                        // possible at all)
                        rename(dialog
                                .getInputEditText()
                                .getText()
                                .toString(), onDupeName);
                    }
                })
                .show();
    }

    private void showDupeNameDialog(final Runnable runOnDupeName) {
        new MaterialDialog.Builder(activity)
                .content("Sorry, you've already got a core by that name, try another one.")
                .theme(Theme.LIGHT)
                .positiveText("OK")
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        if (runOnDupeName != null) {
                            runOnDupeName.run();
                        }
                    }
                }).show();
    }

    private void rename(String newName, Runnable runOnDupeName) {
        Set<String> currentDeviceNames = device.getCloud().getDeviceNames();
        if (currentDeviceNames.contains(newName) || newName.equals(device.getName())) {
            showDupeNameDialog(runOnDupeName);
        } else {
            doRename(newName);
        }
    }

    private void doRename(final String newName) {
        Async.executeAsync(device, new Async.ApiProcedure<SparkDevice>() {
            @Override
            public Void callApi(SparkDevice sparkDevice) throws SparkCloudException, IOException {
                device.setName(newName);
                return null;
            }

            @Override
            public void onFailure(SparkCloudException exception) {
                new MaterialDialog.Builder(activity)
                        .theme(Theme.LIGHT)
                        .title("Unable to rename core")
                        .content(exception.getBestMessage())
                        .positiveText("OK");
            }
        }).andIgnoreCallbacksIfActivityIsFinishing(activity);
    }


}
