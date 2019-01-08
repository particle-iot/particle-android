package io.particle.mesh.common

import mu.KotlinLogging


/**
 * A simple tool for error reporting, to make it easy to ensure that if a problem occurred,
 * *someone* should hear about it (e.g.: via logs, a crash reporter, etc)
 *
 * The implementation is intentionally left open-ended to allow one to
 * plug in whatever behaviors one wants into this tool.
 */
object QATool {

    @Volatile var isDebugBuild: Boolean = false

    @Volatile var implementation: QAToolImpl = DefaultImpl()

    @Suppress("NOTHING_TO_INLINE")  // keep this inlined for cleaner stack traces
    inline fun report(exception: Throwable) {
        implementation.doReport(exception)
    }

    @Suppress("NOTHING_TO_INLINE")  // (same as see above)
    inline fun illegalState(msg: String) {
        implementation.doReport(IllegalStateException(msg))
    }

    @Suppress("NOTHING_TO_INLINE")  // (same as see above)
    inline fun runSafely(vararg funcs: () -> Any) {
        for (func in funcs) {
            try {
                func()
            } catch (error: Throwable) {
                report(error)
            }
        }
    }

    fun log(msg: String) {
        implementation.doLog(msg)
    }

}


interface QAToolImpl {
    fun doLog(msg: String)
    fun doReport(exception: Throwable)
}


private class DefaultImpl : QAToolImpl {

    private val log = KotlinLogging.logger {}

    override fun doLog(msg: String) {
        log.warn { msg }
    }

    override fun doReport(exception: Throwable) {
        log.error(exception) { "Not reporting error from default QATool impl: " }
    }
}
