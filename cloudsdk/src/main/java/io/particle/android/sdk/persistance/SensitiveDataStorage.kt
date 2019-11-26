package io.particle.android.sdk.persistance

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.*
import javax.annotation.ParametersAreNonnullByDefault


interface SensitiveDataStorage {

    val user: String?
    val password: String?
    val token: String?
    val refreshToken: String?
    val tokenExpirationDate: Date?
    val hasEverHadStoredUsername: Boolean

    fun saveUser(user: String?)
    fun resetUser()

    fun savePassword(password: String?)
    fun resetPassword()

    fun saveToken(token: String?)
    fun resetToken()

    fun resetRefreshToken()
    fun saveRefreshToken(token: String?)

    fun saveTokenExpirationDate(expirationDate: Date)
    fun resetTokenExpirationDate()

    fun saveHasEverHadStoredUsername(value: Boolean)
}



private const val KEY_USERNAME = "KEY_USERNAME"
private const val KEY_PASSWORD = "KEY_PASSWORD"
private const val KEY_TOKEN = "KEY_TOKEN"
private const val KEY_TOKEN_EXPIRATION_DATE = "KEY_TOKEN_EXPIRATION_DATE"
private const val KEY_REFRESH_TOKEN = "KEY_REFRESH_TOKEN"
private const val KEY_HAS_EVER_HAD_STORED_USERNAME = "KEY_HAS_EVER_HAD_STORED_USERNAME"


@ParametersAreNonnullByDefault
internal class SensitiveDataStorageImpl(context: Context) : SensitiveDataStorage {

    private val sharedPrefs: SharedPreferences

    init {
        val ctx = context.applicationContext
        sharedPrefs = ctx.getSharedPreferences("spark_sdk_sensitive_data", Context.MODE_PRIVATE)
    }


    override val user: String?
        get() = sharedPrefs.getString(KEY_USERNAME, null)

    override fun saveUser(user: String?) {
        if (user != null && user.isNotEmpty()) {
            saveHasEverHadStoredUsername(true)
        }
        sharedPrefs.edit { putString(KEY_USERNAME, user) }
    }

    override fun resetUser() {
        sharedPrefs.edit { remove(KEY_USERNAME) }
    }


    override val password: String?
        get() = sharedPrefs.getString(KEY_PASSWORD, null)

    override fun savePassword(password: String?) {
        sharedPrefs.edit { putString(KEY_PASSWORD, password) }
    }

    override fun resetPassword() {
        sharedPrefs.edit { remove(KEY_PASSWORD) }
    }


    override val token: String?
        get() = sharedPrefs.getString(KEY_TOKEN, null)

    override fun saveToken(token: String?) {
        sharedPrefs.edit { putString(KEY_TOKEN, token) }
    }

    override fun resetToken() {
        sharedPrefs.edit { remove(KEY_TOKEN) }
    }


    override val refreshToken: String?
        get() = sharedPrefs.getString(KEY_REFRESH_TOKEN, null)

    override fun resetRefreshToken() {
        sharedPrefs.edit { remove(KEY_REFRESH_TOKEN) }
    }

    override fun saveRefreshToken(token: String?) {
        sharedPrefs.edit { putString(KEY_REFRESH_TOKEN, token) }
    }


    override val tokenExpirationDate: Date?
        get() {
            val expirationTs = sharedPrefs.getLong(KEY_TOKEN_EXPIRATION_DATE, -1)
            return if (expirationTs == -1L) null else Date(expirationTs)
        }

    override fun saveTokenExpirationDate(expirationDate: Date) {
        sharedPrefs.edit { putLong(KEY_TOKEN_EXPIRATION_DATE, expirationDate.time) }
    }

    override fun resetTokenExpirationDate() {
        sharedPrefs.edit { remove(KEY_TOKEN_EXPIRATION_DATE) }
    }


    override val hasEverHadStoredUsername: Boolean
        get() = sharedPrefs.getBoolean(
            KEY_HAS_EVER_HAD_STORED_USERNAME,
            false
        )

    override fun saveHasEverHadStoredUsername(value: Boolean) {
        sharedPrefs.edit { putBoolean(KEY_HAS_EVER_HAD_STORED_USERNAME, value) }
    }

}