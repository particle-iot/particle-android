package io.particle.android.sdk


import android.app.Application


fun onApplicationCreated(app: Application) {
    // set debugging properties for Kotlin coroutines here in debug builds
    System.setProperty("DEBUG_PROPERTY_NAME", "BANANA")
    System.setProperty("kotlinx.coroutines.stacktrace.recovery", "true")
}
