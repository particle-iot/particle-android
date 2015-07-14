package io.particle.android.sdk.tinker;

import android.app.Application;

import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.ui.DeviceListActivity;

public class TinkerApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ParticleDeviceSetupLibrary.init(this, DeviceListActivity.class);
    }
}
