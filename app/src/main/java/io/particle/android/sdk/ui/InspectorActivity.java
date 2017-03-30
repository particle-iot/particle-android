package io.particle.android.sdk.ui;

import android.content.Context;
import android.content.Intent;

import io.particle.android.sdk.cloud.ParticleDevice;

/**
 * An activity representing the Inspector screen for a Device.
 */
public class InspectorActivity extends BaseActivity {
    public static final String EXTRA_DEVICE = "EXTRA_DEVICE";

    public static Intent buildIntent(Context ctx, ParticleDevice device) {
        return new Intent(ctx, InspectorActivity.class)
                .putExtra(EXTRA_DEVICE, device);
    }
}
