package io.particle.android.sdk.tinker

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import io.particle.android.sdk.devicesetup.BuildConfig
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary
import io.particle.android.sdk.onApplicationCreated
import io.particle.android.sdk.ui.devicelist.DeviceListActivity
import io.particle.mesh.common.QATool
import mu.KotlinLogging


class TinkerApplication : Application() {

    private val log = KotlinLogging.logger {}

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

        log.info { "Device make and model=${getDeviceNameAndMfg()},\n" +
                "OS version=${Build.VERSION.RELEASE},\n" +
                "App version=$appVersionName,"
        }
    }
}


private fun getDeviceNameAndMfg(): String {
    val manufacturer = Build.MANUFACTURER
    val model = Build.MODEL
    return if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
        model.capitalize()
    } else manufacturer.capitalize() + " " + model
}


private val Context.appVersionName: String
    get() {
        return try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            QATool.report(e)
            "(Error getting version)"
        }
    }