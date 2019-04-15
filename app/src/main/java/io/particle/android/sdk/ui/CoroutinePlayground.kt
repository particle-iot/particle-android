package io.particle.android.sdk.ui

import android.content.Context
import android.provider.Settings.Global
import androidx.lifecycle.MutableLiveData
import androidx.navigation.R.attr
import io.particle.mesh.common.android.livedata.liveDataSuspender
import io.particle.mesh.common.android.livedata.nonNull
import kotlinx.coroutines.*
import mu.KotlinLogging



private val log = KotlinLogging.logger {}


suspend fun <T> CoroutineScope.withTimeoutThatActuallyWorks(
    timeMillis: Long,
    block: suspend CoroutineScope.() -> T): T {
    if (timeMillis <= 0L) throw CancellationException("Timed out immediately")

    val deferred = async { block() }

    GlobalScope.launch {
        log.info { "Beginning timeout watcher" }
        delay(timeMillis)
        if (deferred.isActive) {
            log.info { "Deferred is still active, cancelling..." }
            deferred.cancel()
        }
    }

    return deferred.await()
}


class CoroutinePlayground {

    private val log = KotlinLogging.logger {}

    val job = Job()
    val backgroundScope = CoroutineScope(Dispatchers.Default + job)
    val mainThreadScope = CoroutineScope(Dispatchers.Main + job)

    fun runTest(context: Context) {
        mainThreadScope.async {



//            doRunTest()




        }.invokeOnCompletion { cause ->
            log.info { "Hello, I'm done because of $cause" }
        }
    }

    suspend fun doRunTest() {
        val ld = MutableLiveData<Boolean>()
        val ldSuspender = liveDataSuspender({ ld.nonNull() })

        mainThreadScope.launch {
            log.info { "Beginning 'post to LiveData' delay" }
            delay(4000)
            log.info { "posting to LiveData" }
            ld.postValue(true)
        }

        var lolwut: Boolean? = null

        try {
            lolwut = mainThreadScope.withTimeoutThatActuallyWorks(3000) {
                ldSuspender.awaitResult()
            }
        } catch (ex: CancellationException) {
            log.info { "Cancelled!" }
        }
        log.info { "lolwut = $lolwut" }

//        val jerb = mainThreadScope.launch {
//            log.info { "Beginning awaitResult()" }
//            ldSuspender.awaitResult()
//            delay(500)
//            log.info { "Ending awaitResult()" }
//        }
//        mainThreadScope.launch {
//            log.info { "beginning timeout watcher" }
//            delay(5000)
//            log.info { "timeout watcher delay() complete" }
//            if (jerb.isActive) {
//                log.info { "jerb is active, cancelling jerb" }
//                jerb.cancel()
//            }
//        }

        //        mainThreadScope.launch {
//            val rezult1 = withTimeoutOrNull(2000) {
//                backgroundScope.async {
//                    log.info { "Beginning Thread.sleep()" }
//                    Thread.sleep(5000)
//                    log.info { "Exiting Thread.sleep()" }
//                    "returned after sleep completed"
//                }.await()
//            }
//            log.info { "Rezult1: $rezult1" }
//
//            val rezult2 = withTimeoutOrNull(2000) {
//                backgroundScope.async {
//                    log.info { "Beginning delay()" }
//                    delay(5000)
//                    log.info { "Exiting delay()" }
//                    "returned after delay() completed"
//                }.await()
//            }
//            log.info { "Rezult2: $rezult2" }
//
//
//            val rezult3 = withTimeoutOrNull(2000) {
//                backgroundScope.async {
//                    log.info { "Beginning delay() 2" }
//                    delay(1500)
//                    log.info { "Exiting delay() 2" }
//                    "returned after delay() 2 completed"
//                }.await()
//            }
//            log.info { "Rezult3: $rezult3" }
//
//        }
    }

}