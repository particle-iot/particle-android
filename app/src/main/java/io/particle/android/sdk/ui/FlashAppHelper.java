package io.particle.android.sdk.ui;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.EZ;
import io.particle.sdk.app.R;

public class FlashAppHelper {


    public static void flashAppFromBinaryWithDialog(final FragmentActivity activity,
                                                    final ParticleDevice device, final File path) {
        new MaterialDialog.Builder(activity)
                // FIXME: this is just for flashing Tinker for now, but later it could be used
                // for whatever file the user wants to upload (and "known apps" will work for
                // the Photon, too)
                // .content(String.format("Flash %s?", StringUtils.capitalize(knownApp.getAppName())))
                .content("Flash Tinker?")
                .theme(Theme.LIGHT)
                .positiveText(R.string.flash)
                .negativeText(R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        flashFromBinary(activity, device, path);
                    }
                })
                .show();
    }

    public static void flashPhotonTinkerWithDialog(final FragmentActivity activity,
                                                   final ParticleDevice device) {
        final InputStream inputStream = activity.getResources().openRawResource(R.raw.photon_tinker);
        new MaterialDialog.Builder(activity)
                .content("Flash Tinker?")
                .theme(Theme.LIGHT)
                .positiveText(R.string.flash)
                .negativeText(R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        flashFromStream(activity, device, inputStream, "Tinker");
                    }
                })
                .show();
    }


    public static void flashKnownAppWithDialog(final FragmentActivity activity,
                                               final ParticleDevice device,
                                               final ParticleDevice.KnownApp knownApp) {
        new MaterialDialog.Builder(activity)
                .content(String.format("Flash %s?", StringUtils.capitalize(knownApp.getAppName())))
                .theme(Theme.LIGHT)
                .positiveText(R.string.flash)
                .negativeText(R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        flashKnownApp(activity, device, knownApp);
                    }
                })
                .show();
    }

    // FIXME: remove duplication between the following methods
    private static void flashKnownApp(final Activity activity, final ParticleDevice device,
                                      final ParticleDevice.KnownApp knownApp) {
        Async.executeAsync(device, new Async.ApiProcedure<ParticleDevice>() {
            @Override
            public Void callApi(ParticleDevice sparkDevice) throws ParticleCloudException, IOException {
                device.flashKnownApp(knownApp);
                return null;
            }

            @Override
            public void onFailure(ParticleCloudException exception) {
                new MaterialDialog.Builder(activity)
                        .title("Unable to reflash " + StringUtils.capitalize(knownApp.getAppName()))
                        .content(exception.getBestMessage())
                        .positiveText("OK")
                        .show();
            }
        }).andIgnoreCallbacksIfActivityIsFinishing(activity);
    }

    private static void flashFromBinary(final Activity activity, final ParticleDevice device,
                                        final File binaryFile) {
        // FIXME: incroporate real error handling here
        try {
            FileInputStream fis = new FileInputStream(binaryFile);
            flashFromStream(activity, device, fis, binaryFile.getName());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    private static void flashFromStream(final Activity activity, final ParticleDevice device,
                                        final InputStream stream, final String name) {
        Async.executeAsync(device, new Async.ApiProcedure<ParticleDevice>() {
            @Override
            public Void callApi(ParticleDevice sparkDevice) throws ParticleCloudException, IOException {
                device.flashBinaryFile(stream);
                EZ.closeThisThingOrMaybeDont(stream);
                return null;
            }

            @Override
            public void onFailure(ParticleCloudException exception) {
                new MaterialDialog.Builder(activity)
                        .title("Unable to reflash from " + name)
                        .content(exception.getBestMessage())
                        .positiveText("OK")
                        .show();
            }
        }).andIgnoreCallbacksIfActivityIsFinishing(activity);
    }

}
