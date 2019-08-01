package io.particle.android.sdk.tinker

import android.app.Application
import io.particle.android.sdk.devicesetup.BuildConfig
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary
import io.particle.android.sdk.onApplicationCreated
import io.particle.android.sdk.ui.devicelist.DeviceListActivity
import io.particle.mesh.common.QATool


class TinkerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        QATool.isDebugBuild = BuildConfig.DEBUG

        // HI THERE: doing a release build?  Read the rest of this comment.  (Otherwise, carry on.)
        //
        // ReleaseBuildAppInitializer is a per-build type file, intended to avoid initializing
        // things like analytics when doing debug builds (i.e.: what most people will be doing when
        // they download the app via GitHub.)
        //
        // If you do a release build of an app based on this code, you'll need to manually comment
        // out this line by hand or otherwise prevent calling the code
        // inside ReleaseBuildAppInitializer
        onApplicationCreated(this)

        ParticleDeviceSetupLibrary.init(this, DeviceListActivity::class.java)
    }
}
