package io.particle.android.sdk.tinker;

import android.content.Context;
import android.content.SharedPreferences;

class TinkerPrefs {

    private static final String BUCKET_NAME = "tinkerPrefsBucket";
    private static final String KEY_IS_VISITED = "isVisited";

    private static TinkerPrefs instance = null;


    static TinkerPrefs getInstance(Context ctx) {
        if (instance == null) {
            instance = new TinkerPrefs(ctx);
        }
        return instance;
    }


    private final SharedPreferences prefs;


    private TinkerPrefs(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(BUCKET_NAME, Context.MODE_PRIVATE);
    }

    boolean isFirstVisit() {
        return !prefs.getBoolean(KEY_IS_VISITED, false);
    }

    void setVisited(boolean isVisited) {
        prefs.edit().putBoolean(KEY_IS_VISITED, isVisited).apply();
    }

}