package io.particle.android.sdk.tinker

import android.support.multidex.MultiDexApplication
import io.particle.android.sdk.ReleaseBuildAppInitializer
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary
import io.particle.android.sdk.ui.DeviceListActivity
import io.particle.particlemesh.common.QATool
import org.slf4j.impl.HandroidLoggerAdapter


class TinkerApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        // HI THERE: doing a release build?  Read the rest of this comment.  (Otherwise, carry on.)
        //
        // ReleaseBuildAppInitializer is a per-build type file, intended to avoid initializing
        // things like analytics when doing debug builds (i.e.: what most people will be doing when
        // they download the app via GitHub.)
        //
        // If you do a release build of an app based on this code, you'll need to manually comment
        // out this line by hand or otherwise prevent calling the code
        // inside ReleaseBuildAppInitializer
        ReleaseBuildAppInitializer.onApplicationCreated(this)

        QATool.isDebugBuild = true   //BuildConfig.DEBUG
        HandroidLoggerAdapter.DEBUG = true  //BuildConfig.DEBUG
        //        FirebaseApp.initializeApp(this);

        ParticleDeviceSetupLibrary.init(this, DeviceListActivity::class.java)
    }
}
