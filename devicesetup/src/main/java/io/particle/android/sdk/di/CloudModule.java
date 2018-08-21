package io.particle.android.sdk.di;

import android.support.annotation.RestrictTo;

import com.google.gson.Gson;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudSDK;

@Module
@RestrictTo({RestrictTo.Scope.LIBRARY})
public class CloudModule {

    @Singleton
    @Provides
    protected ParticleCloud providesParticleCloud() {
        return ParticleCloudSDK.getCloud();
    }

    @Singleton
    @Provides
    protected Gson providesGson() {
        return new Gson();
    }
}
