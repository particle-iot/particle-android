package io.particle.android.sdk.ui;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.EZ;
import io.particle.sdk.app.R;


class FlashAppHelper {


    public static void flashAppFromBinaryWithDialog(final FragmentActivity activity,
                                                    final ParticleDevice device, final File path) {
        new AlertDialog.Builder(activity)
                // FIXME: this is just for flashing Tinker for now, but later it could be used
                // for whatever file the user wants to upload (and "known apps" will work for
                // the Photon, too)
                // .content(String.format("Flash %s?", capitalize(knownApp.getAppName())))
                .setMessage("Flash Tinker?")
                .setPositiveButton(R.string.flash, (dialog, which) -> flashFromBinary(
                        activity, device, path))
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }


    static void flashPhotonTinkerWithDialog(final FragmentActivity activity,
                                            final ParticleDevice device) {
        final InputStream inputStream = activity.getResources().openRawResource(R.raw.photon_tinker);
        new AlertDialog.Builder(activity)
                .setMessage("Flash Tinker?")
                .setPositiveButton(R.string.flash, (dialog, which) -> flashFromStream(
                        activity, device, inputStream, "Tinkerrrrr"))
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }


    static void flashKnownAppWithDialog(final FragmentActivity activity,
                                        final ParticleDevice device,
                                        final ParticleDevice.KnownApp knownApp) {
        new AlertDialog.Builder(activity)
                .setMessage(String.format("Flash %s?", capitalize(knownApp.getAppName())))
                .setPositiveButton(R.string.flash, (dialog, which) -> flashKnownApp(
                        activity, device, knownApp))
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }


    // FIXME: remove duplication between the following methods
    private static void flashKnownApp(final Activity activity, final ParticleDevice device,
                                      final ParticleDevice.KnownApp knownApp) {
        try {
            Async.executeAsync(device, new Async.ApiProcedure<ParticleDevice>() {
                @Override
                public Void callApi(@NonNull ParticleDevice sparkDevice) throws ParticleCloudException, IOException {
                    device.flashKnownApp(knownApp);
                    return null;
                }

                @Override
                public void onFailure(@NonNull ParticleCloudException exception) {
                    showFailureMessage(activity, "Unable to reflash " +
                            capitalize(knownApp.getAppName()), exception);
                }
            }).andIgnoreCallbacksIfActivityIsFinishing(activity);
        } catch (ParticleCloudException e) {
            showFailureMessage(activity, "Unable to reflash " + capitalize(knownApp.getAppName()), e);
        }
    }

    private static void flashFromBinary(final Activity activity, final ParticleDevice device,
                                        final File binaryFile) {
        // FIXME: incorporate real error handling here
        try {
            FileInputStream fis = new FileInputStream(binaryFile);
            flashFromStream(activity, device, fis, binaryFile.getName());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    private static void flashFromStream(final Activity activity, final ParticleDevice device,
                                        final InputStream stream, final String name) {
        try {
            Async.executeAsync(device, new Async.ApiProcedure<ParticleDevice>() {
                @Override
                public Void callApi(@NonNull ParticleDevice sparkDevice)
                        throws ParticleCloudException, IOException {
                    sparkDevice.flashBinaryFile(stream);
                    EZ.closeThisThingOrMaybeDont(stream);
                    return null;
                }

                @Override
                public void onFailure(@NonNull ParticleCloudException exception) {
                    showFailureMessage(activity, "Unable to reflash from " + name, exception);
                }
            }).andIgnoreCallbacksIfActivityIsFinishing(activity);
        } catch (ParticleCloudException e) {
            showFailureMessage(activity, "Unable to reflash from " + name, e);
        }
    }

    private static void showFailureMessage(Activity activity, String message, ParticleCloudException exception) {
        new AlertDialog.Builder(activity)
                .setTitle(message)
                .setMessage(exception.getBestMessage())
                .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }

    // lifted from Apache commons-lang StringUtils
    private static String capitalize(final String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }

        final int firstCodepoint = str.codePointAt(0);
        final int newCodePoint = Character.toTitleCase(firstCodepoint);
        if (firstCodepoint == newCodePoint) {
            // already capitalized
            return str;
        }

        final int newCodePoints[] = new int[strLen]; // cannot be longer than the char array
        int outOffset = 0;
        newCodePoints[outOffset++] = newCodePoint; // copy the first codepoint
        for (int inOffset = Character.charCount(firstCodepoint); inOffset < strLen; ) {
            final int codepoint = str.codePointAt(inOffset);
            newCodePoints[outOffset++] = codepoint; // copy the remaining ones
            inOffset += Character.charCount(codepoint);
        }
        return new String(newCodePoints, 0, outOffset);
    }

}
