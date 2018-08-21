package io.particle.devicesetup.exampleapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.util.Date;

import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.utils.ui.Ui;

public class MainActivity extends AppCompatActivity {
    public final static String EXTRA_SETUP_LAUNCHED_TIME = "io.particle.devicesetup.exampleapp.SETUP_LAUNCHED_TIME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ParticleDeviceSetupLibrary.init(this.getApplicationContext());

        Ui.findView(this, R.id.start_setup_button).setOnClickListener(view -> invokeDeviceSetup());
        Ui.findView(this, R.id.start_setup_custom_intent_button).setOnClickListener(v -> invokeDeviceSetupWithCustomIntentBuilder());

        String setupLaunchTime = this.getIntent().getStringExtra(EXTRA_SETUP_LAUNCHED_TIME);

        if (setupLaunchTime != null) {
            TextView label = Ui.findView(this, R.id.textView);

            label.setText(String.format(getString(R.string.welcome_back), setupLaunchTime));
        }
    }

    public void invokeDeviceSetup() {
        ParticleDeviceSetupLibrary.startDeviceSetup(this, MainActivity.class);
    }

    private void invokeDeviceSetupWithCustomIntentBuilder() {
        final String setupLaunchedTime = new Date().toString();

        // Important: don't use an anonymous inner class to implement SetupCompleteIntentBuilder, otherwise you will cause a memory leak.
        ParticleDeviceSetupLibrary.startDeviceSetup(this, new ExampleSetupCompleteIntentBuilder(setupLaunchedTime));
    }

}
