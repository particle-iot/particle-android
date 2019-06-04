package io.particle.android.sdk.cloud


import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.collection.LongSparseArray
import androidx.collection.keyIterator
import com.google.gson.Gson
import io.particle.android.sdk.cloud.ApiDefs.CloudApi
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.android.sdk.utils.Py.truthy
import io.particle.android.sdk.utils.TLog
import org.kaazing.net.sse.SseEventReader
import org.kaazing.net.sse.SseEventSource
import org.kaazing.net.sse.SseEventSourceFactory
import org.kaazing.net.sse.SseEventType
import org.kaazing.net.sse.impl.AuthenticatedEventSourceFactory
import retrofit.RetrofitError
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicLong
import javax.annotation.ParametersAreNonnullByDefault


// See javadoc on ParticleCloud for the intended behavior of these methods
@ParametersAreNonnullByDefault
internal class EventsDelegate(
    private val cloudApi: CloudApi,
    baseApiUri: Uri,
    private val gson: Gson,
    private val executor: ExecutorService,
    cloud: ParticleCloud
) {
    private val uris: EventApiUris
    private val eventSourceFactory: SseEventSourceFactory

    private val subscriptionIdGenerator = AtomicLong(1)
    private val eventReaders = LongSparseArray<EventReader>()

    init {
        this.eventSourceFactory = AuthenticatedEventSourceFactory(cloud)
        this.uris = EventApiUris(baseApiUri)
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun publishEvent(
        eventName: String,
        event: String?,
        @ParticleEventVisibility eventVisibility: Int,
        timeToLive: Int
    ) {
        val isPrivate = eventVisibility != ParticleEventVisibility.PUBLIC
        try {
            cloudApi.publishEvent(eventName, event, isPrivate, timeToLive)
        } catch (error: RetrofitError) {
            throw ParticleCloudException(error)
        }
    }

    @WorkerThread
    @Throws(IOException::class)
    fun subscribeToAllEvents(eventNamePrefix: String?, handler: ParticleEventHandler): Long {
        return subscribeToEventWithUri(uris.buildAllEventsUri(eventNamePrefix), handler)
    }

    @WorkerThread
    @Throws(IOException::class)
    fun subscribeToMyDevicesEvents(eventNamePrefix: String?, handler: ParticleEventHandler): Long {
        return subscribeToEventWithUri(uris.buildMyDevicesEventUri(eventNamePrefix), handler)
    }

    @WorkerThread
    @Throws(IOException::class)
    fun subscribeToDeviceEvents(
        eventNamePrefix: String?,
        deviceID: String,
        eventHandler: ParticleEventHandler
    ): Long {
        return subscribeToEventWithUri(
            uris.buildSingleDeviceEventUri(eventNamePrefix, deviceID),
            eventHandler
        )
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun unsubscribeFromEventWithID(eventListenerID: Long) {
        val reader = synchronized(eventReaders) {
            val theReader = eventReaders.get(eventListenerID)
            eventReaders.remove(eventListenerID)
            return@synchronized theReader
        }

        try {
            reader?.stopListening()
        } catch (e: IOException) {
            // handling the exception here instead of putting it in the method signature
            // is inconsistent, but SDK consumers aren't going to care about receiving
            // this exception, so just swallow it here.
            log.w("Error while trying to stop event listener", e)
        }
    }

    @WorkerThread
    @Throws(ParticleCloudException::class)
    fun unsubscribeFromEventWithHandler(handler: SimpleParticleEventHandler) {
        var readerToStop: EventReader? = null
        synchronized(eventReaders) {
            for (readerId in eventReaders.keyIterator()) {
                val reader = eventReaders[readerId]
                if (reader?.handler == handler) {
                    eventReaders.remove(readerId)
                    readerToStop = reader
                    break
                }
            }
        }

        try {
            readerToStop?.stopListening()
        } catch (e: IOException) {
            // Handling the exception here instead of putting it in the method signature is
            // inconsistent, but SDK consumers aren't going to care about receiving this exception,
            // so just swallow it here.
            log.e("Error while trying to stop event listener", e)
        }
    }

    @Throws(IOException::class)
    private fun subscribeToEventWithUri(uri: Uri, handler: ParticleEventHandler): Long {
        val subscriptionId = subscriptionIdGenerator.getAndIncrement()
        val reader = EventReader(handler, executor, gson, uri, eventSourceFactory)
        // log.d("Created event subscription with ID " + subscriptionId + " for URI " + uri);
        synchronized(eventReaders) {
            eventReaders.put(subscriptionId, reader)
        }
        reader.startListening()
        return subscriptionId
    }


    private class EventReader constructor(
        internal val handler: ParticleEventHandler,
        internal val executor: ExecutorService,
        internal val gson: Gson,
        uri: Uri,
        factory: SseEventSourceFactory
    ) {
        internal val sseEventSource: SseEventSource

        @Volatile
        internal var future: Future<*>? = null

        init {
            try {
                sseEventSource = factory.createEventSource(URI.create(uri.toString()))
            } catch (e: URISyntaxException) {
                // I don't like throwing exceptions in constructors, but this URI shouldn't be in
                // the wrong format...
                throw RuntimeException(e)
            }

        }

        @Throws(IOException::class)
        internal fun startListening() {
            sseEventSource.connect()
            val sseEventReader = sseEventSource.eventReader
            try {
                future = executor.submit { startHandlingEvents(sseEventReader) }
            } catch (ex: Exception) {
                // FIXME: fix this a better way instead of just the band-aid
            }

        }

        @Throws(IOException::class)
        internal fun stopListening() {
            future?.cancel(false)
            sseEventSource.close()
        }


        private fun startHandlingEvents(sseEventReader: SseEventReader) {
            var type: SseEventType?
            try {
                type = sseEventReader.next()
                while (type != SseEventType.EOS) {

                    if (type != null && type == SseEventType.DATA) {
                        val data = sseEventReader.data
                        val asStr = data.toString()

                        val event = gson.fromJson(asStr, ParticleEvent::class.java)

                        try {
                            handler.onEvent(sseEventReader.name, event)
                        } catch (ex: Exception) {
                            handler.onEventError(ex)
                        }

                    } else {
                        log.w("type null or not data: " + type!!)
                    }
                    type = sseEventReader.next()
                }
            } catch (e: IOException) {
                handler.onEventError(e)
            }

        }
    }

    private class EventApiUris internal constructor(baseUri: Uri) {

        private val EVENTS = "events"

        private val allEventsUri: Uri
        private val devicesBaseUri: Uri
        private val myDevicesEventsUri: Uri

        init {
            allEventsUri = baseUri.buildUpon().path("/v1/$EVENTS").build()
            devicesBaseUri = baseUri.buildUpon().path("/v1/devices").build()
            myDevicesEventsUri = devicesBaseUri.buildUpon().appendPath(EVENTS).build()
        }

        internal fun buildAllEventsUri(eventNamePrefix: String?): Uri {
            return if (truthy(eventNamePrefix)) {
                allEventsUri.buildUpon().appendPath(eventNamePrefix).build()
            } else {
                allEventsUri
            }
        }

        internal fun buildMyDevicesEventUri(eventNamePrefix: String?): Uri {
            return if (truthy(eventNamePrefix)) {
                myDevicesEventsUri.buildUpon().appendPath(eventNamePrefix).build()
            } else {
                myDevicesEventsUri
            }
        }

        internal fun buildSingleDeviceEventUri(eventNamePrefix: String?, deviceId: String): Uri {
            val builder = devicesBaseUri.buildUpon()
                .appendPath(deviceId)
                .appendPath(EVENTS)
            if (truthy(eventNamePrefix)) {
                builder.appendPath(eventNamePrefix)
            }
            return builder.build()
        }
    }

    companion object {

        private val log = TLog.get(EventsDelegate::class.java)
    }

}
