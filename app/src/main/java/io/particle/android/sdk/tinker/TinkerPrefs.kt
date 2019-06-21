package io.particle.android.sdk.tinker

import android.content.Context
import android.content.SharedPreferences

internal class TinkerPrefs private constructor(context: Context) {


    private val prefs: SharedPreferences

    val isFirstVisit: Boolean
        get() = !prefs.getBoolean(KEY_IS_VISITED, false)


    init {
        prefs = context.applicationContext
            .getSharedPreferences(BUCKET_NAME, Context.MODE_PRIVATE)
    }

    fun setVisited(isVisited: Boolean) {
        prefs.edit().putBoolean(KEY_IS_VISITED, isVisited).apply()
    }

    companion object {

        private val BUCKET_NAME = "tinkerPrefsBucket"
        private val KEY_IS_VISITED = "isVisited"

        private var instance: TinkerPrefs? = null


        fun getInstance(ctx: Context): TinkerPrefs {
            if (instance == null) {
                instance = TinkerPrefs(ctx)
            }
            return instance
        }
    }

}