package io.particle.android.sdk.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.SDKGlobals;
import io.particle.android.sdk.utils.EZ;
import io.particle.android.sdk.utils.TLog;
import io.particle.sdk.app.R;


/**
 * More than just a way to display a splash screen; this also wraps
 * FirstRealActivitySelector for you, and launches the appropriate activity
 * from there.
 */
public class SplashActivity extends BaseActivity {

    private static final TLog log = TLog.get(SplashActivity.class);

    // How long the branded splash (logo on navy) is held for a returning, logged-in user
    // before we continue to the device list, so it reads as a clean logo screen rather
    // than just flashing by. New users skip it and go straight to the intro screen.
    private static final long SPLASH_DISPLAY_TIME_MS = 2000;

    private boolean isReturningUser;
    private boolean finished = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isReturningUser = SDKGlobals.getAppDataStorage().getUserHasClaimedDevices();

        if (!isReturningUser) {
            // New user: no branded splash needed, the intro screen already shows the logo.
            onShowingSplashComplete();
            return;
        }

        this.setContentView(R.layout.activity_splash);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override
    public void onStart() {
        super.onStart();
        // For new users we've already routed onward from onCreate().
        if (!isReturningUser) {
            return;
        }

        if (finished) {
            onShowingSplashComplete();
            return;
        }

        EZ.runOnMainThreadDelayed(SPLASH_DISPLAY_TIME_MS, () -> {
            finished = true;
            onShowingSplashComplete();
        });
    }

    private void onShowingSplashComplete() {
        if (isFinishing()) {
            log.i("Activity is already finished/finishing, not launching next Activity");

        } else {
            Intent intent;
            if (SDKGlobals.getAppDataStorage().getUserHasClaimedDevices()) {
                intent = NextActivitySelector.getNextActivityIntent(this,
                        ParticleCloudSDK.getCloud(),
                        SDKGlobals.getSensitiveDataStorage(),
                        null);
            } else {
                intent = new Intent(this, IntroActivity.class);
            }

            log.d("Splash screen done, moving to next Activity: " + intent);
            startActivity(intent);
            finish();
        }
    }

}
