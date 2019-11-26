package io.particle.android.sdk.cloud

import android.content.Context
import io.particle.android.sdk.persistance.AppDataStorage
import io.particle.android.sdk.persistance.AppDataStorageImpl
import io.particle.android.sdk.persistance.SensitiveDataStorage

object SDKGlobals {
    @Volatile
    var sensitiveDataStorage: SensitiveDataStorage? = null
    @Volatile
    var appDataStorage: AppDataStorage? = null
    private var isInitialized = false
    @Synchronized
    fun init(context: Context) {
        val ctx = context.applicationContext
        if (isInitialized) {
            return
        }
        sensitiveDataStorage = SensitiveDataStorage(ctx)
        appDataStorage = AppDataStorageImpl(ctx)
        isInitialized = true
    }

}