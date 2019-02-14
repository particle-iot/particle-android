package io.particle.mesh.common

import kotlinx.coroutines.*
import mu.KotlinLogging


class ActionDebouncer(
        private val debouncePeriodMillis: Long,
        private val debouncedAction: () -> Unit
) {

    private val log = KotlinLogging.logger {}


    private val lockObj = Any()

    @Volatile
    private var delayed: Job? = null

    @Volatile
    private var lastRunTimestamp: Long = 0


    fun executeDebouncedAction() {
        synchronized(lockObj) {
            executeOrScheduleAction()
        }
    }

    fun cancelScheduledAction() {
        synchronized(lockObj) {
            delayed?.let {
                log.debug { "Canceling delayed job" }
                delayed?.cancel()
            }
            delayed = null
        }
    }

    private fun executeOrScheduleAction() {
        if (delayed != null) {
            log.debug { "Delayed job already scheduled; doing nothing." }
            return
        }

        val now = System.currentTimeMillis()
        val nextAllowableTimeToExecute = lastRunTimestamp + debouncePeriodMillis

        // Have we passed the next allowed time passed yet?
        if (now > nextAllowableTimeToExecute) {
            // yes: do it.
            runDebouncedAction()
            return
        }

        // Not yet. Create a pending job to do it later.
        val delay = nextAllowableTimeToExecute - now
        log.info { "Haven't passed debounce time yet.  Scheduling job with delay=$delay ms" }
        scheduleRunningDebouncedJob(delay)
    }

    private fun runDebouncedAction() {
        debouncedAction()
        lastRunTimestamp = System.currentTimeMillis()
    }

    private fun scheduleRunningDebouncedJob(delayMillis: Long) {
        delayed = GlobalScope.launch(Dispatchers.Default) {
            delay(delayMillis)
            synchronized(lockObj) {
                runDebouncedAction()
                delayed = null
            }
        }
    }

}
