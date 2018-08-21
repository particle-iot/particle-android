package io.particle.android.sdk.di;

import android.app.Application;
import android.content.Context;
import android.support.annotation.RestrictTo;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
@RestrictTo({RestrictTo.Scope.LIBRARY})
public class ApplicationModule {
    private Application application;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public ApplicationModule(Application application) {
        this.application = application;
    }

    @Singleton
    @Provides
    protected Application providesApplication() {
        return application;
    }

    @Singleton
    @Provides
    protected Context providesContext() {
        return application;
    }

}
