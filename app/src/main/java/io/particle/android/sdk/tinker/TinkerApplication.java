package io.particle.android.sdk.tinker;

import android.support.multidex.MultiDexApplication;

import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;

public class TinkerApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        ParticleDeviceSetupLibrary.init(this);
    }
}
