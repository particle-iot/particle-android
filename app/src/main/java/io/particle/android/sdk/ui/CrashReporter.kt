package io.particle.android.sdk.ui

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Process
import io.particle.sdk.app.BuildConfig
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess


/**
 * Catches otherwise-uncaught exceptions and shows [CrashReportActivity] with a shareable stack
 * trace + device/build info, then terminates. Installed from TinkerApplication. Replaces the
 * remote crash reporting (Crashlytics) that was removed, giving testers a way to send us the exact
 * crash from a sideloaded build.
 */
class CrashReporter(
    private val app: Application,
    private val previousHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            app.startActivity(
                Intent(app, CrashReportActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .putExtra(CrashReportActivity.EXTRA_REPORT, buildReport(thread, throwable))
            )
        } catch (ignored: Throwable) {
            // If we can't show the report, fall back to the default handler below.
            previousHandler?.uncaughtException(thread, throwable)
            return
        }
        // The report activity runs in its own process; tear this one down.
        Process.killProcess(Process.myPid())
        exitProcess(10)
    }

    private fun buildReport(thread: Thread, throwable: Throwable): String {
        val stack = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        return buildString {
            appendLine("Particle Android crash report")
            appendLine("App: ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE % 100})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Thread: ${thread.name}")
            appendLine()
            append(stack)
        }
    }
}
