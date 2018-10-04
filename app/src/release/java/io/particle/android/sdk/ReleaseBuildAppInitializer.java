package io.particle.android.sdk;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.segment.analytics.Analytics;
import com.segment.analytics.android.integrations.firebase.FirebaseIntegration;
import com.segment.analytics.android.integrations.intercom.IntercomIntegration;

import io.fabric.sdk.android.Fabric;

import org.jetbrains.annotations.NotNull;

import android.util.Log;

import io.particle.android.sdk.utils.GDPRKt;

import io.particle.mesh.common.QATool;
import io.particle.mesh.common.QAToolImpl;


public class ReleaseBuildAppInitializer {

    public static void onApplicationCreated(Application app) {
        boolean coveredByGDPR = GDPRKt.isUserCoveredByGDPR();

        if (!coveredByGDPR) {
            //"MVP" level GDPR support: only enable crash reporting if the user is NOT in the EU.
            Fabric.with(app, new Crashlytics());
        }

        QATool.INSTANCE.setImplementation(new QAToolImpl() {

            @Override
            public void doReport(@NotNull Throwable exception) {
                if (coveredByGDPR) {
                    return;
                }
                Log.e("ParticleApp", "Sending error to Crashlytics:", exception);
                Crashlytics.logException(exception);
            }

            @Override
            public void doLog(@NotNull String msg) {
                if (coveredByGDPR) {
                    return;
                }
                Crashlytics.log(msg);
            }
        });

//        String fakeKey = "lolnope12345";  // FIXME: use real key
        // Disable, even in prod, until we have the key
//        Analytics.setSingletonInstance(new Analytics.Builder(app, fakeKey)
//                .use(FirebaseIntegration.FACTORY)
//                .use(IntercomIntegration.FACTORY)
//                .build()
//        );
    }

}
