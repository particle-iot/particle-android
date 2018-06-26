package io.particle.android.sdk;

import android.app.Application;

import com.segment.analytics.Analytics;
import com.segment.analytics.android.integrations.firebase.FirebaseIntegration;
import com.segment.analytics.android.integrations.intercom.IntercomIntegration;


public class ReleaseBuildAppInitializer {

    public static void onApplicationCreated(Application app) {
//        String fakeKey = "lolnope12345";  // FIXME: use real key
        // Disable, even in prod, until we have the key
//        Analytics.setSingletonInstance(new Analytics.Builder(app, fakeKey)
//                .use(FirebaseIntegration.FACTORY)
//                .use(IntercomIntegration.FACTORY)
//                .build()
//        );
    }

}
