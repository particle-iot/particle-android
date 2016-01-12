package io.particle.android.sdk.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;

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

//@Param
public class FlashAppHelper {


    public static void flashAppFromBinaryWithDialog(final FragmentActivity activity,
                                                    final ParticleDevice device, final File path) {
        new AlertDialog.Builder(activity)
                // FIXME: this is just for flashing Tinker for now, but later it could be used
                // for whatever file the user wants to upload (and "known apps" will work for
                // the Photon, too)
                // .content(String.format("Flash %s?", StringUtils.capitalize(knownApp.getAppName())))
                .setMessage("Flash Tinker?")
                .setPositiveButton(R.string.flash, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        flashFromBinary(activity, device, path);
                    }
                })
                .setNegativeButton(R.string.cancel, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    public static void flashPhotonTinkerWithDialog(final FragmentActivity activity,
                                                   final ParticleDevice device) {
        final InputStream inputStream = activity.getResources().openRawResource(R.raw.photon_tinker);
        new AlertDialog.Builder(activity)
                .setMessage("Flash Tinker?")
                .setPositiveButton(R.string.flash, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        flashFromStream(activity, device, inputStream, "Tinker");
                    }
                })
                .setNegativeButton(R.string.cancel, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }


    public static void flashKnownAppWithDialog(final FragmentActivity activity,
                                               final ParticleDevice device,
                                               final ParticleDevice.KnownApp knownApp) {
        new AlertDialog.Builder(activity)
                .setMessage(String.format("Flash %s?", StringUtils.capitalize(knownApp.getAppName())))
                .setPositiveButton(R.string.flash, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        flashKnownApp(activity, device, knownApp);
                    }
                })
                .setNegativeButton(R.string.cancel, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
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
                new AlertDialog.Builder(activity)
                        .setTitle("Unable to reflash " + StringUtils.capitalize(knownApp.getAppName()))
                        .setMessage(exception.getBestMessage())
                        .setPositiveButton(R.string.ok, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        }).andIgnoreCallbacksIfActivityIsFinishing(activity);
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
        Async.executeAsync(device, new Async.ApiProcedure<ParticleDevice>() {
            @Override
            public Void callApi(@NonNull ParticleDevice sparkDevice)
                    throws ParticleCloudException, IOException {
                sparkDevice.flashBinaryFile(stream);
                EZ.closeThisThingOrMaybeDont(stream);
                return null;
            }

            @Override
            public void onFailure(ParticleCloudException exception) {
                new AlertDialog.Builder(activity)
                        .setTitle("Unable to reflash from " + name)
                        .setMessage(exception.getBestMessage())
                        .setPositiveButton(R.string.ok, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        }).andIgnoreCallbacksIfActivityIsFinishing(activity);
    }

}
