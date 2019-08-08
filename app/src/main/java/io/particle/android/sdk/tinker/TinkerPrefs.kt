package io.particle.android.sdk.tinker

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences


private const val BUCKET_NAME = "tinkerPrefsBucket"
private const val KEY_IS_VISITED = "isVisited"
private const val KEY_API_BASE_URI = "KEY_API_BASE_URI"


internal class TinkerPrefs private constructor(context: Context) {

    companion object {

        @SuppressLint("StaticFieldLeak")  // we use app context, so it doesn't matter
        private var instance: TinkerPrefs? = null

        fun getInstance(ctx: Context): TinkerPrefs {
            var inst = instance
            if (inst == null) {
                inst = TinkerPrefs(ctx)
                instance = inst
            }
            return inst
        }
    }

    private val app: Context = context.applicationContext
    private val prefs: SharedPreferences =
        app.getSharedPreferences(BUCKET_NAME, Context.MODE_PRIVATE)

    val isFirstVisit: Boolean
        get() = !prefs.getBoolean(KEY_IS_VISITED, false)

//    val apiBaseUri: String?
//        get() = _getApiBaseUri()

    fun setVisited(isVisited: Boolean) {
        prefs.edit().putBoolean(KEY_IS_VISITED, isVisited).apply()
    }

//    private fun _getApiBaseUri(): String? {
//        return
//    }

}
