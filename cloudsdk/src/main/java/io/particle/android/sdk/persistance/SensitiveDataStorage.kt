package io.particle.android.sdk.persistance

import android.content.Context
import android.content.SharedPreferences
import java.util.*
import javax.annotation.ParametersAreNonnullByDefault

// FIXME: crib the code from the Vault example to do crypto for all these values.
@ParametersAreNonnullByDefault
class SensitiveDataStorage(ctx: Context) {
    private val sharedPrefs: SharedPreferences
    fun saveUser(user: String?) {
        if (user != null && !user.isEmpty()) {
            saveHasEverHadStoredUsername(true)
        }
        sharedPrefs.edit()
            .putString(KEY_USERNAME, user)
            .apply()
    }

    val user: String
        get() = sharedPrefs.getString(KEY_USERNAME, null)

    fun resetUser() {
        sharedPrefs.edit()
            .remove(KEY_USERNAME)
            .apply()
    }

    fun savePassword(password: String?) {
        sharedPrefs.edit()
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    val password: String
        get() = sharedPrefs.getString(KEY_PASSWORD, null)

    fun resetPassword() {
        sharedPrefs.edit()
            .remove(KEY_PASSWORD)
            .apply()
    }

    fun saveToken(token: String?) {
        sharedPrefs.edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }

    val token: String
        get() = sharedPrefs.getString(KEY_TOKEN, null)

    fun resetToken() {
        sharedPrefs.edit()
            .remove(KEY_TOKEN)
            .apply()
    }

    fun resetRefreshToken() {
        sharedPrefs.edit()
            .remove(KEY_REFRESH_TOKEN)
            .apply()
    }

    fun saveRefreshToken(token: String?) {
        sharedPrefs.edit()
            .putString(KEY_REFRESH_TOKEN, token)
            .apply()
    }

    val refreshToken: String?
        get() = sharedPrefs.getString(KEY_REFRESH_TOKEN, null)

    fun saveTokenExpirationDate(expirationDate: Date) {
        sharedPrefs.edit()
            .putLong(
                KEY_TOKEN_EXPIRATION_DATE,
                expirationDate.time
            )
            .apply()
    }

    val tokenExpirationDate: Date?
        get() {
            val expirationTs =
                sharedPrefs.getLong(KEY_TOKEN_EXPIRATION_DATE, -1)
            return if (expirationTs == -1L) null else Date(expirationTs)
        }

    fun resetTokenExpirationDate() {
        sharedPrefs.edit()
            .remove(KEY_TOKEN_EXPIRATION_DATE)
            .apply()
    }

    val hasEverHadStoredUsername: Boolean
        get() = sharedPrefs.getBoolean(
            KEY_HAS_EVER_HAD_STORED_USERNAME,
            false
        )

    private fun saveHasEverHadStoredUsername(value: Boolean) {
        sharedPrefs.edit()
            .putBoolean(KEY_HAS_EVER_HAD_STORED_USERNAME, value)
            .apply()
    }

    companion object {
        private const val KEY_USERNAME = "KEY_USERNAME"
        private const val KEY_PASSWORD = "KEY_PASSWORD"
        private const val KEY_TOKEN = "KEY_TOKEN"
        private const val KEY_TOKEN_EXPIRATION_DATE = "KEY_TOKEN_EXPIRATION_DATE"
        private const val KEY_REFRESH_TOKEN = "KEY_REFRESH_TOKEN"
        private const val KEY_HAS_EVER_HAD_STORED_USERNAME =
            "KEY_HAS_EVER_HAD_STORED_USERNAME"
    }

    init {
        var ctx = ctx
        ctx = ctx.applicationContext
        sharedPrefs = ctx.getSharedPreferences(
            "spark_sdk_sensitive_data",
            Context.MODE_PRIVATE
        )
    }
}