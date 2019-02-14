package io.particle.mesh.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.particle.mesh.common.QATool
import io.particle.mesh.common.android.SimpleLifecycleOwner
import mu.KotlinLogging
import java.util.concurrent.ConcurrentLinkedQueue


// FIXME: the right way to get notified of successful packet writes is via a Channel!

@Volatile
private var HANDLER_THREAD_ID = 1
// FIXME: think more carefully about these numbers.  What should they be?
private const val DELAY_BETWEEN_SENDS_MILLIS = 50
private const val WRITE_ATTEMPTS_BEFORE_CLOSING_CONNECTION = 10



class BTCharacteristicWriter(
        private val gatt: BluetoothGatt,
        private val writeCharacteristic: BluetoothGattCharacteristic,
        private val onCharacteristicWrittenLD: LiveData<GATTStatusCode>
) {

    private val log = KotlinLogging.logger {}

    private val packetQueue = ConcurrentLinkedQueue<ByteArray>()

    private val lifecycleOwner = SimpleLifecycleOwner()

    private val handlerThread = HandlerThread("BLE_WRITE_HANDLER_" + HANDLER_THREAD_ID++)
    private val workerThreadHandler: Handler

    @Volatile
    private var writeAttempts: Int = 0
    @Volatile
    private var active = true
    @Volatile
    private var dequeScheduled: Boolean = false

    init {
        handlerThread.start()
        workerThreadHandler = Handler(handlerThread.looper)
        lifecycleOwner.setNewState(Lifecycle.State.RESUMED)
        onCharacteristicWrittenLD.observe(lifecycleOwner, Observer {
            onCharacteristicWritten()
        })
    }

    fun deactivate() {
        active = false
        lifecycleOwner.setNewState(Lifecycle.State.DESTROYED)
        onCharacteristicWrittenLD.removeObservers(lifecycleOwner)
        packetQueue.clear()
        handlerThread.quitSafely()
    }

    fun writeToCharacteristic(value: ByteArray) {
        if (!active) {
            QATool.illegalState("Asked to accept command data on deactivated write channel")
            return
        }

        packetQueue.add(value)
        scheduleDeque()
    }

    private fun onCharacteristicWritten() {
        workerThreadHandler.post { this.deque() }
    }

    @Synchronized
    private fun scheduleDeque() {
        workerThreadHandler.post { this.doScheduleDeque() }
    }

    @Synchronized
    private fun doScheduleDeque() {
        if (dequeScheduled) {
            return
        }
        dequeScheduled = true
        workerThreadHandler.postDelayed({
            this.deque()
        }, DELAY_BETWEEN_SENDS_MILLIS.toLong())
    }

    @WorkerThread
    private fun deque() {
        dequeScheduled = false

        if (!active) {
            QATool.log("Running deque op on inactive write channel!")
            return
        }

        // retrieve the head from the queue WITHOUT removing it
        val packet = packetQueue.peek() ?: return

        val errMsg = writePacket(packet)

        if (errMsg == null) { // success
            writeAttempts = 0
            packetQueue.poll()  // like .remove() but doesn't throw if the queue is empty

        } else {

            writeAttempts++
            if (writeAttempts > WRITE_ATTEMPTS_BEFORE_CLOSING_CONNECTION) {
                log.error { "Exceeded maximum write attempts; invalidating connection!" }
                return
            }

            val msg = "Error writing packet: $errMsg, writeAttempts=$writeAttempts"
            if (writeAttempts > 3) {
                log.warn { msg }
            } else {
                log.trace { msg }
            }

            scheduleDeque()
        }

        // the "onCharacteristicWritten()" call will prompt us to write the next packet...
    }

    private fun writePacket(packet: ByteArray): String? {
        val valueSet = writeCharacteristic.setValue(packet)
        if (!valueSet) {
            return "Unable to set value on write characteristic"
        }

        val writeInitiated = gatt.writeCharacteristic(writeCharacteristic)
        return if (writeInitiated) null else "Unable to write characteristic to BLE GATT"
    }

}