package io.particle.android.sdk.devicesetup;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.Nullable;

public interface SetupCompleteIntentBuilder {
    Intent buildIntent(Context ctx, @Nullable SetupResult result);
}
