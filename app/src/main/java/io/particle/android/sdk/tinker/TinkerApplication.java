package io.particle.android.sdk.tinker;

import android.support.multidex.MultiDexApplication;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import io.fabric.sdk.android.Fabric;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.ui.DeviceListActivity;
import io.particle.sdk.app.BuildConfig;

public class TinkerApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        CrashlyticsCore core = new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build();
        Fabric.with(this, new Crashlytics.Builder().core(core).build());
        ParticleDeviceSetupLibrary.init(this, DeviceListActivity.class);
    }
}
