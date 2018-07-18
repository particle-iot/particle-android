package io.particle.android.sdk;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.segment.analytics.Analytics;
import com.segment.analytics.android.integrations.firebase.FirebaseIntegration;
import com.segment.analytics.android.integrations.intercom.IntercomIntegration;

import org.jetbrains.annotations.NotNull;

import io.particle.particlemesh.common.QATool;
import io.particle.particlemesh.common.QAToolImpl;


public class ReleaseBuildAppInitializer {

    public static void onApplicationCreated(Application app) {
        QATool.INSTANCE.setImplementation(new QAToolImpl() {

            @Override
            public void doReport(@NotNull Throwable exception) {
                Crashlytics.logException(exception);
            }

            @Override
            public void doLog(@NotNull String msg) {
                Crashlytics.log(msg);
            }
        });


        String fakeKey = "lolnope12345";  // FIXME: use real key
        Analytics.setSingletonInstance(new Analytics.Builder(app, fakeKey)
                .use(FirebaseIntegration.FACTORY)
                .use(IntercomIntegration.FACTORY)
                .build()
        );
    }

}
