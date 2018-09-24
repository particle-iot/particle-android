package io.particle.android.sdk.ui;


import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException;
import io.particle.android.sdk.utils.Async;
import io.particle.sdk.app.R;


class RenameHelper {

    static void renameDevice(FragmentActivity activity, ParticleDevice device) {
        new RenameHelper(device, activity).showDialog();
    }

    private final ParticleDevice device;
    private final FragmentActivity activity;

    private RenameHelper(ParticleDevice device, FragmentActivity activity) {
        this.device = device;
        this.activity = activity;
    }


    private void showDialog() {
        // FIXME: include "suggest different name" button
        // FIXME: device name cache is gone, re-implement this later.
//        Set<String> noNames = Collections.emptySet();
//        final String suggestedName = CoreNameGenerator.generateUniqueName(noNames);
        final String suggestedName = device.getName();

        new MaterialDialog.Builder(activity)
                .title("Rename Device")
                .content("Set new name for your device:")
                .theme(Theme.LIGHT)
                .input("Name your device", suggestedName, (dialog, input) -> {
                    // FIXME: do validation here to prevent short names?
                    // I think this is already done cloud-side.  Verify.
                })
                .positiveText("Rename")
                .negativeText("Cancel")
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        Runnable onDupeName = () -> showDialog();

                        EditText inputText = dialog.getInputEditText();
                        if (inputText == null) {
                            return;
                        }
                        rename(inputText.getText().toString(), onDupeName);
                    }
                })
                .show();
    }

    private void showDupeNameDialog(final Runnable runOnDupeName) {
        new AlertDialog.Builder(activity)
                .setMessage("Sorry, you've already got a core by that name, try another one.")
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    if (runOnDupeName != null) {
                        runOnDupeName.run();
                    }
                })
                .show();
    }

    private void rename(String newName, Runnable runOnDupeName) {
        // FIXME: device name cache is gone, re-implement this later.
//        Set<String> currentDeviceNames = device.getCloud().getDeviceNames();
        Set<String> currentDeviceNames = Collections.emptySet();
        if (currentDeviceNames.contains(newName) || newName.equals(device.getName())) {
            showDupeNameDialog(runOnDupeName);
        } else {
            doRename(newName);
        }
    }

    private void doRename(final String newName) {
        try {
            Async.executeAsync(device, new Async.ApiProcedure<ParticleDevice>() {
                @Override
                public Void callApi(@NonNull ParticleDevice sparkDevice) throws ParticleCloudException, IOException {
                    device.setName(newName);
                    EventBus.getDefault().post(device);
                    return null;
                }

                @Override
                public void onFailure(@NonNull ParticleCloudException exception) {
                    new MaterialDialog.Builder(activity)
                            .theme(Theme.LIGHT)
                            .title("Unable to rename core")
                            .content(exception.getBestMessage())
                            .positiveText("OK");
                }
            }).andIgnoreCallbacksIfActivityIsFinishing(activity);
        } catch (ParticleCloudException e) {
            new MaterialDialog.Builder(activity)
                    .theme(Theme.LIGHT)
                    .title("Unable to rename core")
                    .content(e.getBestMessage())
                    .positiveText("OK");
        }
    }

}
