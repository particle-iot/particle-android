package io.particle.android.sdk.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;

import io.particle.android.sdk.tinker.PinAction;

import static io.particle.android.sdk.utils.Py.list;


public class Prefs {

    private static final String BUCKET_NAME = "particleAppPrefsBucket";

    private static final String KEY_COMPLETED_FIRST_LOGIN = "completedFirstLogin";
    private static final String KEY_CORES_JSON_ARRAY = "coresJsonArray";
    private static final String KEY_PIN_CONFIG_TEMPLATE = "corePinConfig_core-$1%s_pin-$2%s";


    private static Prefs instance = null;


    public static Prefs getInstance(Context ctx) {
        if (instance == null) {
            instance = new Prefs(ctx);
        }

        return instance;
    }


    private final SharedPreferences prefs;


    private Prefs(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(BUCKET_NAME, Context.MODE_PRIVATE);
    }

    public boolean getCompletedFirstLogin() {
        return prefs.getBoolean(KEY_COMPLETED_FIRST_LOGIN, false);
    }

    public void saveCompletedFirstLogin(boolean value) {
        prefs.edit().putBoolean(KEY_COMPLETED_FIRST_LOGIN, value).commit();
    }

    public String getCoresJsonArray() {
        return prefs.getString(KEY_CORES_JSON_ARRAY, "[]");
    }

    public void saveCoresJsonArray(String coresJson) {
        saveString(KEY_CORES_JSON_ARRAY, coresJson);
    }


    public PinAction getPinFunction(String coreId, String pinName) {
        String key = String.format(KEY_PIN_CONFIG_TEMPLATE, coreId, pinName);
        return PinAction.valueOf(
                prefs.getString(key, PinAction.NONE.name()));
    }

    public void savePinFunction(String coreId, String pinName, PinAction function) {
        String key = String.format(KEY_PIN_CONFIG_TEMPLATE, coreId, pinName);
        applyString(key, function.name());
    }

    public void clearTinker(String coreId) {
        List<String> pinNames = list("A0", "A1", "A2", "A3", "A4", "A5", "A6", "A7", "D0", "D1",
                "D2", "D3", "D4", "D5", "D6", "D7");
        for (String pinName : pinNames) {
            savePinFunction(coreId, pinName, PinAction.NONE);
        }
    }

    public void clear() {
        boolean completed = getCompletedFirstLogin();
        prefs.edit().clear().commit();
        saveCompletedFirstLogin(completed);
    }

    private void saveString(String key, String value) {
        prefs.edit().putString(key, value).commit();
    }

    private void applyString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

}