package io.particle.android.sdk.devicesetup;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.LocalBroadcastManager;

import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.devicesetup.ui.GetReadyActivity;
import io.particle.android.sdk.di.ApplicationComponent;
import io.particle.android.sdk.di.ApplicationModule;
import io.particle.android.sdk.di.DaggerApplicationComponent;
import io.particle.android.sdk.ui.BaseActivity;
import io.particle.android.sdk.utils.Preconditions;


public class ParticleDeviceSetupLibrary {
    private ApplicationComponent applicationComponent;

    /**
     * The contract for the broadcast sent upon device setup completion.
     * <p/>
     * <em>NOTE: this broadcast will be sent via the LocalBroadcastManager</em>
     * <p/>
     * The DeviceSetupCompleteReceiver class, which wraps up this logic, has been provided as a
     * convenience.
     */
    public interface DeviceSetupCompleteContract {

        /**
         * The BroadcastIntent action sent when the device setup process is complete.
         */
        String ACTION_DEVICE_SETUP_COMPLETE = "ACTION_DEVICE_SETUP_COMPLETE";

        /**
         * A boolean extra indicating if the setup was successful
         */
        String EXTRA_DEVICE_SETUP_WAS_SUCCESSFUL = "EXTRA_DEVICE_SETUP_WAS_SUCCESSFUL";

        /**
         * A long extra indicating the device ID of the configured device.
         * <p/>
         * Value is undefined if EXTRA_DEVICE_SETUP_WAS_SUCCESSFUL is false.
         */
        String EXTRA_CONFIGURED_DEVICE_ID = "EXTRA_CONFIGURED_DEVICE_ID";
    }


    /**
     * A convenience class which wraps DeviceSetupCompleteContract.
     * <p/>
     * Just extend this and override onSetupSuccess() and onSetupFailure() to receive
     * the success/failure status.
     */
    public static abstract class DeviceSetupCompleteReceiver extends BroadcastReceiver {

        public abstract void onSetupSuccess(@NonNull String configuredDeviceId);

        // FIXME: add some extra error information in onSetupFailed()
        public abstract void onSetupFailure();


        /**
         * Optional convenience method for registering this receiver.
         */
        public void register(Context ctx) {
            LocalBroadcastManager.getInstance(ctx).registerReceiver(this, buildIntentFilter());
        }

        /**
         * Optional convenience method for registering this receiver.
         */
        public void unregister(Context ctx) {
            LocalBroadcastManager.getInstance(ctx).unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean success = intent.getBooleanExtra(
                    DeviceSetupCompleteContract.EXTRA_DEVICE_SETUP_WAS_SUCCESSFUL, false);
            String deviceId = intent.getStringExtra(DeviceSetupCompleteContract.EXTRA_CONFIGURED_DEVICE_ID);
            if (success && deviceId != null) {
                onSetupSuccess(deviceId);
            } else {
                onSetupFailure();
            }
        }

        public IntentFilter buildIntentFilter() {
            return new IntentFilter(DeviceSetupCompleteContract.ACTION_DEVICE_SETUP_COMPLETE);
        }
    }


    /**
     * Start the device setup process.
     *
     * @deprecated Use {@link ParticleDeviceSetupLibrary#startDeviceSetup(Context, Class)}
     * or {@link ParticleDeviceSetupLibrary#startDeviceSetup(Context, SetupCompleteIntentBuilder)} instead.
     */
    @Deprecated
    public static void startDeviceSetup(Context ctx) {
        Preconditions.checkNotNull(instance.setupCompleteIntentBuilder,
                "SetupCompleteIntentBuilder instance is null");

        ctx.startActivity(new Intent(ctx, GetReadyActivity.class));
    }

    public static void startDeviceSetup(Context ctx, SetupCompleteIntentBuilder setupCompleteIntentBuilder) {
        instance.setupCompleteIntentBuilder = setupCompleteIntentBuilder;

        startDeviceSetup(ctx);
    }

    public static void startDeviceSetup(Context ctx, final Class<? extends Activity> mainActivity) {
        startDeviceSetup(ctx, (ctx1, result) -> new Intent(ctx1, mainActivity));
    }


    // FIXME: allow the SDK consumer to optionally pass in some kind of dynamic intent builder here
    // instead of a static class
    // FIXME: or, stop requiring an activity at all and just use a single activity for setup which
    // uses Fragments internally...

    /**
     * Initialize the device setup SDK
     *
     * @param ctx          any Context (the application context will be accessed from whatever is
     *                     passed in here, so leaks are not a concern even if you pass in an
     *                     Activity here)
     * @param mainActivity the class for your 'main" activity, i.e.: the class you want to
     *                     return to when the setup process is complete.
     * @deprecated Use {@link ParticleDeviceSetupLibrary#init(Context)} with
     * {@link ParticleDeviceSetupLibrary#startDeviceSetup(Context, Class)}
     * or {@link ParticleDeviceSetupLibrary#startDeviceSetup(Context, SetupCompleteIntentBuilder)} instead.
     */
    @Deprecated
    public static void init(Context ctx, final Class<? extends Activity> mainActivity) {
        init(ctx);

        instance.setupCompleteIntentBuilder = (ctx1, result) -> new Intent(ctx1, mainActivity);
    }

    /**
     * Initialize the device setup SDK
     *
     * @param ctx any Context (the application context will be accessed from whatever is
     *            passed in here, so leaks are not a concern even if you pass in an
     *            Activity here)
     */
    public static void init(Context ctx) {
        if (instance == null) {
            // ensure the cloud SDK is initialized
            ParticleCloudSDK.init(ctx);
            instance = new ParticleDeviceSetupLibrary();
            instance.setComponent(DaggerApplicationComponent
                    .builder()
                    .applicationModule(new ApplicationModule((Application) ctx.getApplicationContext()))
                    .build());
        }
    }

    /**
     * Initialize the device setup SDK for setup only (setup flow will bypass any authentication and device claiming)
     *
     * @param ctx any Context (the application context will be accessed from whatever is
     *            passed in here, so leaks are not a concern even if you pass in an
     *            Activity here)
     */
    public static void initWithSetupOnly(Context ctx) {
        BaseActivity.setupOnly = true;
        init(ctx);
    }

    public static ParticleDeviceSetupLibrary getInstance() {
        Preconditions.checkNotNull(instance,
                "Library instance is null: did you call ParticleDeviceSetupLibrary.init()?");
        return instance;
    }

    private static ParticleDeviceSetupLibrary instance;

    private SetupCompleteIntentBuilder setupCompleteIntentBuilder;

    /**
     * @deprecated This exists only because {@link io.particle.android.sdk.ui.NextActivitySelector}
     * is in a different package, and the plan is to remove that class soon.
     * As soon as {@link io.particle.android.sdk.ui.NextActivitySelector} is removed, this will be removed.
     */
    @Deprecated
    public SetupCompleteIntentBuilder getSetupCompleteIntentBuilder() {
        return setupCompleteIntentBuilder;
    }

    private ParticleDeviceSetupLibrary() {
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public ApplicationComponent getApplicationComponent() {
        return applicationComponent;
    }

    @VisibleForTesting
    public void setComponent(ApplicationComponent applicationComponent) {
        this.applicationComponent = applicationComponent;
    }
}
