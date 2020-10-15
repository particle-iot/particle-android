package io.particle.android.sdk.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.RestrictTo;
import androidx.appcompat.app.AppCompatActivity;

import io.github.inflationx.calligraphy3.CalligraphyConfig;
import io.github.inflationx.calligraphy3.CalligraphyInterceptor;
import io.github.inflationx.viewpump.ViewPump;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import io.particle.android.sdk.cloud.SDKGlobals;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.utils.SEGAnalytics;


/**
 * This class exists solely to avoid requiring SDK users to have to define
 * anything in an Application subclass.  By (ab)using this custom Activity,
 * we can at least be sure that the custom fonts in the device setup screens
 * work correctly without any special instructions.
 */
// this is a base activity, it shouldn't be registered.
@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static boolean setupOnly = false;
    private static boolean customFontInitDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!customFontInitDone) {
            ViewPump.init(ViewPump.builder()
                .addInterceptor(new CalligraphyInterceptor(
                        new CalligraphyConfig.Builder()
                                .setDefaultFontPath(getString(R.string.normal_text_font_name))
                                .setFontAttrId(R.attr.fontPath)
                                .build()))
                .build());
            customFontInitDone = true;
        }
        SEGAnalytics.initialize(getApplicationContext());
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
        SDKGlobals.init(this);
    }

}
