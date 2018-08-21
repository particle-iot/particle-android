package io.particle.android.sdk.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.RestrictTo;

import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;


/**
 * Created by Julius.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SEGAnalytics {
    public static String analyticsKey = "";
    public static boolean analyticsOptOut = true;
    @SuppressLint("StaticFieldLeak") private static Context context;

    public static void initialize(Context context) {
        SEGAnalytics.context = context.getApplicationContext();
        try {
            Analytics.with(context);
        } catch (IllegalArgumentException exception) {
            if (!analyticsKey.isEmpty()) {
                Analytics analytics = new Analytics.Builder(context, analyticsKey).build();
                analytics.optOut(analyticsOptOut);
                Analytics.setSingletonInstance(analytics);
            }
        }
    }

    public static void track(String track) {
        if (!analyticsKey.isEmpty()) {
            Analytics.with(context).track(track);
        }
    }

    public static void screen(String screen) {
        if (!analyticsKey.isEmpty()) {
            Analytics.with(context).track(screen);
        }
    }

    public static void track(String track, Properties analyticProperties) {
        if (!analyticsKey.isEmpty()) {
            Analytics.with(context).track(track, analyticProperties);
        }
    }

    public static void identify(String email) {
        if (!analyticsKey.isEmpty()) {
            Analytics.with(context).identify(email);
        }
    }
}
