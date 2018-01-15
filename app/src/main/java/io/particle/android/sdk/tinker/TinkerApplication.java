package io.particle.android.sdk.tinker;

import android.support.multidex.MultiDexApplication;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import io.fabric.sdk.android.Fabric;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;

public class TinkerApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics.Builder().core(new CrashlyticsCore.Builder().build()).build());
        ParticleDeviceSetupLibrary.init(this);
    }
}
