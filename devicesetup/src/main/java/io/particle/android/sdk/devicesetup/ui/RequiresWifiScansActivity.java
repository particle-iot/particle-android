package io.particle.android.sdk.devicesetup.ui;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.util.Log;

import io.particle.android.sdk.ui.BaseActivity;

@SuppressLint("Registered")
public class RequiresWifiScansActivity extends BaseActivity {

    @Override
    protected void onStart() {
        super.onStart();
        if (!PermissionsFragment.hasPermission(this, permission.ACCESS_FINE_LOCATION)) {
            Log.d("RequiresWifiScans", "Location permission appears to have been revoked, finishing activity...");
            finish();
        }
    }
}
