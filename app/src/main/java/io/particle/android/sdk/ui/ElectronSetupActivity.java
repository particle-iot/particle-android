package io.particle.android.sdk.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import javax.annotation.ParametersAreNonnullByDefault;

import io.particle.sdk.app.R;


@ParametersAreNonnullByDefault
public class ElectronSetupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_electron_setup);
        // if the action bar is null, something has gone terribly wrong
        //noinspection ConstantConditions
//        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

}
