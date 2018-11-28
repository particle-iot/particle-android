package io.particle.mesh.common

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.newFixedThreadPoolContext


private val ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

/**
 * Run functions on a dedicated worker thread just for local IO work
 */
fun ioThread(f : () -> Unit) {
    ioExecutor.execute(f)
}

// The "executors" for Kotlin's coroutines
val DISKIO = ioExecutor.asCoroutineDispatcher()
val NETWORK = newFixedThreadPoolContext(2, "NETWORK")
