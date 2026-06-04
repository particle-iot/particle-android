package pl.brightinventions.slf4android

import java.util.logging.LogRecord as JulLogRecord

/**
 * Minimal in-tree stub replacing slf4android's LogRecord wrapper. Wraps a
 * [java.util.logging.LogRecord] and exposes the bits [MessageValueSupplier] needs.
 */
class LogRecord(val record: JulLogRecord) {

    companion object {
        @JvmStatic
        fun fromRecord(record: JulLogRecord): LogRecord = LogRecord(record)
    }
}
