package io.particle.android.sdk.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import io.particle.android.sdk.cloud.SDKGlobals;
import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.utils.ui.Ui;
import io.particle.sdk.app.R;


public class IntroActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        String version = "?.?.?";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Ui.setText(this, R.id.version, "v" + version);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }

        Ui.findView(this, R.id.set_up_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = NextActivitySelector.getNextActivityIntent(
                        view.getContext(),
                        ParticleCloud.get(view.getContext()),
                        SDKGlobals.getSensitiveDataStorage(),
                        null);
                startActivity(intent);
                finish();
            }
        });
    }

}
