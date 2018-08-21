package io.particle.android.sdk.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.RestrictTo;
import android.support.v7.app.AppCompatActivity;

import io.particle.android.sdk.cloud.SDKGlobals;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.utils.SEGAnalytics;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

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
        SEGAnalytics.initialize(getApplicationContext());
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        if (!customFontInitDone) {
            // FIXME: make actually customizable via resources
            // (see file extension string formatting nonsense)
            CalligraphyConfig.initDefault(
                    new CalligraphyConfig.Builder()
                            .setDefaultFontPath(newBase.getString(R.string.normal_text_font_name))
                            .setFontAttrId(R.attr.fontPath)
                            .build());
            customFontInitDone = true;
        }
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
        SDKGlobals.init(this);
    }

}
