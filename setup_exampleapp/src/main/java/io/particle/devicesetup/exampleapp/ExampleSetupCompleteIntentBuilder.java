package io.particle.devicesetup.exampleapp;

import android.content.Context;
import android.content.Intent;

import io.particle.android.sdk.devicesetup.SetupCompleteIntentBuilder;
import io.particle.android.sdk.devicesetup.SetupResult;

public class ExampleSetupCompleteIntentBuilder implements SetupCompleteIntentBuilder {
    private final String setupLaunchedTime;

    ExampleSetupCompleteIntentBuilder(String setupLaunchedTime) {
        this.setupLaunchedTime = setupLaunchedTime;
    }

    @Override
    public Intent buildIntent(Context ctx, SetupResult result) {
        Intent intent = new Intent(ctx, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_SETUP_LAUNCHED_TIME, setupLaunchedTime);

        return intent;
    }
}
