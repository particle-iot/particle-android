package io.particle.android.sdk.di;

import android.app.Application;
import android.content.Context;
import android.support.annotation.RestrictTo;

import com.google.gson.Gson;

import javax.inject.Singleton;

import dagger.Component;
import io.particle.android.sdk.cloud.ParticleCloud;

@Singleton
@Component(modules = {ApplicationModule.class, CloudModule.class})
@RestrictTo({RestrictTo.Scope.LIBRARY})
public interface ApplicationComponent {
    ActivityInjectorComponent.Builder activityComponentBuilder();

    Application getApplication();

    Context getContext();

    ParticleCloud getParticleCloud();

    Gson getGson();
}
