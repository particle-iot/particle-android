package io.particle.mesh.setup.utils

import android.os.Looper
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch


// save this value here as a micro-optimization
private val mainLooper = Looper.getMainLooper()


fun isThisTheMainThread(): Boolean = mainLooper === Looper.myLooper()

/** @throws [IllegalStateException] if not called from the main thread */
fun checkIsThisTheMainThread() {
    check(isThisTheMainThread()) { "Not on the main thread!" }
}

/**
 * Executes the runnable immediately if called from the main thread,
 * otherwise, it will be posted to the main thread.
 */
fun runOnMainThread(runnable: () -> Unit) {
    if (isThisTheMainThread()) {
        runnable()
    } else {
        launch(UI) { runnable() }
    }
}
