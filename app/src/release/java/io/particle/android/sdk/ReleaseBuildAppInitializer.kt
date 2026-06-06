package io.particle.android.sdk


import android.app.Application
import android.util.Log

import io.particle.mesh.common.QATool
import io.particle.mesh.common.QAToolImpl
import pl.brightinventions.slf4android.LoggerConfiguration


fun onApplicationCreated(app: Application) {
    // Add file logger (used for the on-device logs).
    LoggerConfiguration.configuration()
        .addHandlerToRootLogger(LoggerConfiguration.fileLogHandler(app))

    // Firebase Analytics / Crashlytics / Performance have been removed from this app.
    // Errors and logs reported through QATool just go to Logcat.
    QATool.implementation = object : QAToolImpl {

        override fun doReport(exception: Throwable) {
            Log.e("Particle error reported", "Error: ", exception)
        }

        override fun doLog(msg: String) {
            Log.e("Particle", msg)
        }
    }
}
