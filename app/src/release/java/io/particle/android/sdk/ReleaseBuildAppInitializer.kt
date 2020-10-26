package io.particle.android.sdk


//import com.segment.analytics.Analytics;
//import com.segment.analytics.android.integrations.firebase.FirebaseIntegration;
//import com.segment.analytics.android.integrations.intercom.IntercomIntegration;

import android.app.Application
import android.util.Log

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.analytics.FirebaseAnalytics

import java.util.logging.Handler
import java.util.logging.Level

import io.particle.android.sdk.utils.*
import io.particle.mesh.common.QATool
import io.particle.mesh.common.QAToolImpl
import pl.brightinventions.slf4android.LogRecord
import pl.brightinventions.slf4android.LoggerConfiguration
import pl.brightinventions.slf4android.MessageValueSupplier
import com.google.firebase.FirebaseApp


fun onApplicationCreated(app: Application) {
    val coveredByGDPR = isUserCoveredByGDPR()
    FirebaseApp.initializeApp(app)

    if (!coveredByGDPR) {
        //"MVP" level GDPR support: only enable crash reporting + Analytics if the user is NOT in the EU.
        FirebaseAnalytics.getInstance(app).setAnalyticsCollectionEnabled(true)


        // Add Crashlytics logger
        LoggerConfiguration.configuration()
            .removeRootLogcatHandler() // remove this because Crashlytics logs to logcat, too
            .addHandlerToRootLogger(CrashlyticsLoggerHandler())
    }

    // Add file logger
    LoggerConfiguration.configuration()
        .addHandlerToRootLogger(LoggerConfiguration.fileLogHandler(app))

    QATool.implementation = object : QAToolImpl {

        override fun doReport(exception: Throwable) {
            Log.e("Particle error reported", "Error: ", exception)
            if (coveredByGDPR) {
                return
            }
            FirebaseCrashlytics.getInstance().recordException(exception)
        }

        override fun doLog(msg: String) {
            Log.e("Particle", msg)
            if (coveredByGDPR) {
                return
            }
            FirebaseCrashlytics.getInstance().log(msg)
        }
    }

//        String fakeKey = "lolnope12345";  // FIXME: use real key
//        // Disable, even in prod, until we have the key
//        Analytics.setSingletonInstance(Analytics.Builder(app, fakeKey)
//            .use(FirebaseIntegration.FACTORY)
//            .use(IntercomIntegration.FACTORY)
//            .build()
//        )
}


fun updateUsernameWithCrashlytics(username: String?) {
    val coveredByGDPR = isUserCoveredByGDPR()
    if (coveredByGDPR) {
        return
    }

    username?.let { FirebaseCrashlytics.getInstance().setUserId(it) }
}


internal class CrashlyticsLoggerHandler : Handler() {

    private var messageValueSupplier = MessageValueSupplier()

    override fun publish(record: java.util.logging.LogRecord) {
        if (record.level.intValue() < Level.FINE.intValue()) {
            return
        }
        // don't include OkHttp's HTTP/2 FrameLogger output
        if (record.loggerName == "com.squareup.okhttp.internal.framed.Http2\$FrameLogger") {
            return
        }
        val logRecord = LogRecord.fromRecord(record)
        val messageBuilder = StringBuilder()
        messageValueSupplier.append(logRecord, messageBuilder)
        FirebaseCrashlytics.getInstance().log(messageBuilder.toString())
    }

    override fun close() {}

    override fun flush() {}
}
