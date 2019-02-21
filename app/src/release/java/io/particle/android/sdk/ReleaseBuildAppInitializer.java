package io.particle.android.sdk;


//import com.segment.analytics.Analytics;
//import com.segment.analytics.android.integrations.firebase.FirebaseIntegration;
//import com.segment.analytics.android.integrations.intercom.IntercomIntegration;

import android.app.Application;
import com.crashlytics.android.Crashlytics;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.logging.Handler;
import pl.brightinventions.slf4android.LogRecord;
import pl.brightinventions.slf4android.MessageValueSupplier;
import pl.brightinventions.slf4android.LoggerConfiguration;

import io.fabric.sdk.android.Fabric;

import org.jetbrains.annotations.NotNull;

import java.util.logging.Handler;
import java.util.logging.Level;

import android.util.Log;

import io.particle.android.sdk.utils.GDPRKt;

import io.particle.mesh.common.QATool;
import io.particle.mesh.common.QAToolImpl;


public class ReleaseBuildAppInitializer {

    public static void onApplicationCreated(Application app) {
        boolean coveredByGDPR = GDPRKt.isUserCoveredByGDPR();

        if (!coveredByGDPR) {
            //"MVP" level GDPR support: only enable crash reporting + Analytics if the user is NOT in the EU.
            FirebaseAnalytics.getInstance(app).setAnalyticsCollectionEnabled(true);
            Fabric.with(app, new Crashlytics());

            // Add Crashlytics logger
            LoggerConfiguration.configuration()
                    .removeRootLogcatHandler() // remove this because Crashlytics logs to logcat, too
                    .addHandlerToRootLogger(new CrashlyticsLoggerHandler());
        }

        // Add file logger
        LoggerConfiguration.configuration().addHandlerToRootLogger(LoggerConfiguration.fileLogHandler(app));

        QATool.INSTANCE.setImplementation(new QAToolImpl() {

            @Override
            public void doReport(@NotNull Throwable exception) {
                if (coveredByGDPR) {
                    Log.e("Particle", "Error: ", exception);
                    return;
                }
                Crashlytics.logException(exception);
            }

            @Override
            public void doLog(@NotNull String msg) {
                if (coveredByGDPR) {
                    Log.e("Particle", msg);
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




    public static class CrashlyticsLoggerHandler extends Handler {

        MessageValueSupplier messageValueSupplier = new MessageValueSupplier();

        @Override
        public void publish(java.util.logging.LogRecord record) {
            if (record.getLevel().intValue() < Level.FINE.intValue()) {
                return;
            }
            // don't include OkHttp's HTTP/2 FrameLogger output
            if (record.getLoggerName().equals("com.squareup.okhttp.internal.framed.Http2$FrameLogger")) {
                return;
            }
            LogRecord logRecord = LogRecord.fromRecord(record);
            StringBuilder messageBuilder = new StringBuilder();
            messageValueSupplier.append(logRecord, messageBuilder);
            String tag = record.getLoggerName();
            int androidLogLevel = logRecord.getLogLevel().getAndroidLevel();
            Crashlytics.log(androidLogLevel, tag, messageBuilder.toString());
        }

        @Override
        public void close() {
        }

        @Override
        public void flush() {
        }
    }

}


