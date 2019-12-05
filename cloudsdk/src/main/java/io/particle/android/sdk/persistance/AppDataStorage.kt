package io.particle.android.sdk.persistance

import android.content.Context
import android.content.SharedPreferences
import javax.annotation.ParametersAreNonnullByDefault


private const val KEY_USER_HAS_CLAIMED_DEVICES = "KEY_USER_HAS_CLAIMED_DEVICES"


interface AppDataStorage {
    val userHasClaimedDevices: Boolean
    fun saveUserHasClaimedDevices(value: Boolean)
    fun resetUserHasClaimedDevices()
}

/**
 * Storage for misc settings to be persisted which **aren't** related to
 * identity, authorization, or any other sensitive data.
 */
@ParametersAreNonnullByDefault
class AppDataStorageImpl(context: Context) : AppDataStorage {

    private val sharedPrefs: SharedPreferences

    init {
        val ctx = context.applicationContext
        sharedPrefs = ctx.getSharedPreferences("spark_sdk_prefs", Context.MODE_PRIVATE)
    }

    override val userHasClaimedDevices: Boolean
        get() = sharedPrefs.getBoolean(KEY_USER_HAS_CLAIMED_DEVICES, false)

    override fun saveUserHasClaimedDevices(value: Boolean) {
        sharedPrefs.edit()
            .putBoolean(KEY_USER_HAS_CLAIMED_DEVICES, value)
            .apply()
    }

    override fun resetUserHasClaimedDevices() {
        sharedPrefs.edit()
            .remove(KEY_USER_HAS_CLAIMED_DEVICES)
            .apply()
    }

}