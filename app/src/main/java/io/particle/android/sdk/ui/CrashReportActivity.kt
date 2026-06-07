package io.particle.android.sdk.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.core.content.getSystemService
import io.particle.sdk.app.R


/**
 * A minimal, self-contained screen shown when the app hits an uncaught exception (see
 * [CrashReporter]). It runs in its own `:crash` process so it survives the dying main process,
 * displays the stack trace, and lets the user copy or share it. This gives crash visibility for
 * sideloaded/beta builds now that Firebase Crashlytics has been removed.
 */
class CrashReportActivity : Activity() {

    companion object {
        const val EXTRA_REPORT = "crash_report_text"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash_report)

        val report = intent.getStringExtra(EXTRA_REPORT) ?: "(no crash details)"
        findViewById<TextView>(R.id.crash_report_text).text = report

        findViewById<Button>(R.id.crash_copy_button).setOnClickListener {
            getSystemService<ClipboardManager>()
                ?.setPrimaryClip(ClipData.newPlainText("Particle crash report", report))
        }

        findViewById<Button>(R.id.crash_share_button).setOnClickListener {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Particle Android crash report")
                putExtra(Intent.EXTRA_TEXT, report)
            }
            startActivity(Intent.createChooser(share, "Share crash report"))
        }
    }
}
