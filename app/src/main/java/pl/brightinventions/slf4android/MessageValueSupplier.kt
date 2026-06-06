package pl.brightinventions.slf4android

/**
 * Minimal in-tree stub replacing slf4android's MessageValueSupplier. Appends the log record's
 * message to the supplied builder (enough for the Crashlytics log handler that formats records).
 */
class MessageValueSupplier {

    fun append(logRecord: LogRecord, builder: StringBuilder) {
        builder.append(logRecord.record.message ?: "")
    }
}
