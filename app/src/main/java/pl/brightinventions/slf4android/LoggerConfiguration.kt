package pl.brightinventions.slf4android

import android.content.Context
import java.util.logging.Handler
import java.util.logging.LogRecord as JulLogRecord

/**
 * Minimal in-tree stub replacing the slf4android dependency (com.github.bright:slf4android),
 * which was JCenter-only and pulled legacy com.android.support artifacts.
 *
 * SLF4J logging itself is provided by the org.slf4j:slf4j-android binding (→ android.util.Log).
 * This stub only satisfies the slf4android-specific configuration API the app calls; the
 * java.util.logging handler routing slf4android used to do is a no-op here. Any java.util.logging
 * [Handler] the app registers (e.g. the Crashlytics handler, the file handler) is accepted but not
 * wired to a logger, so file logging is inert until a real logging backend is reinstated.
 */
class LoggerConfiguration private constructor() {

    fun addHandlerToRootLogger(handler: Handler): LoggerConfiguration = this

    fun removeRootLogcatHandler(): LoggerConfiguration = this

    companion object {

        private val INSTANCE = LoggerConfiguration()

        @JvmStatic
        fun configuration(): LoggerConfiguration = INSTANCE

        @JvmStatic
        fun fileLogHandler(context: Context): FileLogHandlerConfiguration =
            FileLogHandlerConfiguration()
    }
}

/**
 * Stub for slf4android's file log handler. It is a real [Handler] so it can be passed to
 * [LoggerConfiguration.addHandlerToRootLogger], but it discards records.
 */
class FileLogHandlerConfiguration : Handler() {

    fun setFullFilePathPattern(pattern: String): FileLogHandlerConfiguration = this

    override fun publish(record: JulLogRecord?) {}

    override fun flush() {}

    override fun close() {}
}
