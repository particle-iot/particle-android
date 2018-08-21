package io.particle.devicesetup.testapp;

import android.app.Application;
import android.support.test.InstrumentationRegistry;

import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.di.ApplicationComponent;
import io.particle.android.sdk.di.ApplicationModule;
import it.cosenonjaviste.daggermock.DaggerMockRule;

/**
 * Created by Julius.
 */

public class EspressoDaggerMockRule extends DaggerMockRule<ApplicationComponent> {
    public EspressoDaggerMockRule() {
        super(ApplicationComponent.class, new ApplicationModule(getApp()));
        set(component -> {
            ParticleDeviceSetupLibrary.init(getApp());
            ParticleDeviceSetupLibrary.getInstance().setComponent(component);
        });
    }

    private static Application getApp() {
        return (Application) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
    }
}
