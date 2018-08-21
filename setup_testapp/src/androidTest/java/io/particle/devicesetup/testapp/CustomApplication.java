package io.particle.devicesetup.testapp;

import android.app.Application;

import io.particle.android.sdk.cloud.ParticleCloudSDK;

public class CustomApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ParticleCloudSDK.init(this);
    }
}
