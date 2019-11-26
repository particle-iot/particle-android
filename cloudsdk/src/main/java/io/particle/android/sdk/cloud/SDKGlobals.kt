package io.particle.android.sdk.cloud

import android.content.Context
import io.particle.android.sdk.persistance.AppDataStorage
import io.particle.android.sdk.persistance.AppDataStorageImpl
import io.particle.android.sdk.persistance.SensitiveDataStorage
import io.particle.android.sdk.persistance.SensitiveDataStorageImpl

object SDKGlobals {

    @JvmStatic
    @Volatile
    var sensitiveDataStorage: SensitiveDataStorage? = null
        internal set

    @JvmStatic
    @Volatile
    var appDataStorage: AppDataStorage? = null
        internal set

    private var isInitialized = false

    @JvmStatic
    @Synchronized
    fun init(context: Context) {
        val ctx = context.applicationContext
        if (isInitialized) {
            return
        }
        sensitiveDataStorage = SensitiveDataStorageImpl(ctx)
        appDataStorage = AppDataStorageImpl(ctx)
        isInitialized = true
    }

}