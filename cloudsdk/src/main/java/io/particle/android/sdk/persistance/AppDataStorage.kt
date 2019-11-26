package io.particle.android.sdk.persistance

import android.content.Context
import android.content.SharedPreferences
import javax.annotation.ParametersAreNonnullByDefault

/**
 * Storage for misc settings to be persisted which **aren't** related to
 * identity, authorization, or any other sensitive data.
 */
@ParametersAreNonnullByDefault
class AppDataStorage(ctx: Context) {
    private val sharedPrefs: SharedPreferences
    fun saveUserHasClaimedDevices(value: Boolean) {
        sharedPrefs.edit()
            .putBoolean(KEY_USER_HAS_CLAIMED_DEVICES, value)
            .apply()
    }

    val userHasClaimedDevices: Boolean
        get() = sharedPrefs.getBoolean(KEY_USER_HAS_CLAIMED_DEVICES, false)

    fun resetUserHasClaimedDevices() {
        sharedPrefs.edit()
            .remove(KEY_USER_HAS_CLAIMED_DEVICES)
            .apply()
    }

    companion object {
        private const val KEY_USER_HAS_CLAIMED_DEVICES = "KEY_USER_HAS_CLAIMED_DEVICES"
    }

    init {
        var ctx = ctx
        ctx = ctx.applicationContext
        sharedPrefs =
            ctx.getSharedPreferences("spark_sdk_prefs", Context.MODE_PRIVATE)
    }
}