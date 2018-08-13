package io.particle.particlemesh.meshsetup.connection

import io.particle.particlemesh.common.QATool
import io.particle.particlemesh.common.toHex
import io.particle.particlemesh.meshsetup.putUntilFull
import io.particle.particlemesh.meshsetup.readByte
import io.particle.particlemesh.meshsetup.readByteArray
import mu.KotlinLogging
import java.nio.ByteBuffer
import java.nio.ByteOrder


// There's no known max size, but data larger than 10KB seems absurd, so we're going with that.
internal const val MAX_FRAME_SIZE = 10240


// this class exists exclusively to make the pipeline more type safe,
// and resistant to being improperly constructed
class Frame(val frameData: ByteArray)


// (see above re: type safety)
class BlePacket(val data: ByteArray)


class FrameReader(
        var headerBytes: Int,
        private val frameConsumer: (Frame) -> Unit
) {

    private val log = KotlinLogging.logger {}

    private var inProgressFrame: InProgressFrame? = null

    @Synchronized
    fun receivePacket(blePacket: BlePacket) {
        log.debug { "Processing packet: ${blePacket.data.toHex()}" }
        try {
            if (inProgressFrame == null) {
                inProgressFrame = InProgressFrame(headerBytes)
            }

            inProgressFrame!!.writePacket(blePacket.data)

            if (inProgressFrame!!.isComplete) {
                handleCompleteFrame()
            }

        } catch (ex: Exception) {
            inProgressFrame = null // toss out old frame to reset us for the next one
            QATool.report(ex)
        }
    }

    private fun handleCompleteFrame() {
        val ipf = inProgressFrame!!
        val completeFrame = Frame(ipf.consumeFrameData())
        inProgressFrame = null
        frameConsumer(completeFrame)
    }
}


class InProgressFrame(private val headerBytes: Int) {

    var isComplete = false
        private set

    private var frameSize: Short? = null
    private var finalFrameData: ByteArray? = null

    private val packetBuffer = ByteBuffer.allocate(MAX_FRAME_SIZE).order(ByteOrder.LITTLE_ENDIAN)
    // note that this field gets its limit set below
    private val frameDataBuffer = ByteBuffer.allocate(MAX_FRAME_SIZE)

    private var isConsumed = false

    @Synchronized
    fun writePacket(packet: ByteArray) {
        ensureFrameIsNotReused()

        packetBuffer.put(packet)
        packetBuffer.flip()

        if (frameSize == null) {
            onFirstPacket()
        }

        frameDataBuffer.putUntilFull(packetBuffer)

        if (!frameDataBuffer.hasRemaining()) {
            isComplete = true
            onComplete()
            // ensure the rest of the packet was just zeros
            checkRemainderOfPacketIsZeros()
        }

        packetBuffer.clear()
    }

    @Synchronized
    fun consumeFrameData(): ByteArray {
        require(isComplete) { "Cannot consume data, frame is not complete!" }

        ensureFrameIsNotReused()
        isConsumed = true

        return finalFrameData!!
    }

    private fun onFirstPacket() {
        // add the header bytes because, unfortunately, the size field here reflects
        // the *payload size*, not the *remaining frame size*, and the messaging stack
        // has to handle the header bytes inbetween for itself...
        frameSize = (headerBytes + packetBuffer.short).toShort()
        require(frameSize!! >= 0) { "Invalid frame size: $frameSize" }
        frameDataBuffer.limit(frameSize!!.toInt())
    }

    private fun onComplete() {
        frameDataBuffer.flip()
        finalFrameData = frameDataBuffer.readByteArray(frameSize!!.toInt())
    }

    // Ensure that the remainder of a packet is all zeros
    // (this is a sanity check to make ensure we don't receive bytes where we shouldn't)
    private fun checkRemainderOfPacketIsZeros() {
        if (!QATool.isDebugBuild) {
            return
        }

        while (packetBuffer.hasRemaining()) {
            val nextByte = packetBuffer.readByte().toInt()
            require(nextByte == 0) {
                val b = nextByte.toByte().toHex()
                val remainder = packetBuffer.readByteArray().toHex()
                "Found non-zero bytes in the remainder of a packet: byte=$b, remainder=$remainder"
            }
        }
    }

    private fun ensureFrameIsNotReused() {
        if (isConsumed) {
            throw IllegalStateException("Frame already consumed!  Instances cannot be reused!")
        }
    }
}
