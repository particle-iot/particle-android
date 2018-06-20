package io.particle.particlemesh.meshsetup

import io.particle.particlemesh.common.QATool
import io.particle.particlemesh.common.toHex
import mu.KotlinLogging
import java.nio.ByteBuffer
import java.nio.ByteOrder


data class RequestFrame(
        val requestId: Short,
        val messageType: Short,
        val payloadData: ByteArray
) {

    init {
        // ID "0" is reserved, shouldn't be used by a client
        require(requestId != 0.toShort())
    }
}


data class ResponseFrame(
        val requestId: Short,
        val resultCode: Short,
        val payloadData: ByteArray
)


// There's no known max size, but data larger than 10KB seems absurd, so we're going with that.
private val MAX_FRAME_SIZE = 10240


class FrameReader(private val frameConsumer: (ResponseFrame) -> Unit) {

    private val log = KotlinLogging.logger {}

    private var inProgressFrame: FrameInProgress? = null

    @Synchronized
    fun receivePacket(blePacket: ByteArray) {
        log.info { "Processing packet: ${blePacket.toHex()}" }
        try {
            if (inProgressFrame == null) {
                inProgressFrame = FrameInProgress()
            }

            inProgressFrame!!.writePacket(blePacket)

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
        val completeFrame = ResponseFrame(
                ipf.requestId!!,
                ipf.resultCode!!,
                ipf.consumeFrameData()
        )
        inProgressFrame = null
        frameConsumer(completeFrame)
    }
}


class FrameWriter(
        private val byteSink: (ByteArray) -> Unit
) {

    private val buffer = ByteBuffer.allocate(MAX_FRAME_SIZE).order(ByteOrder.LITTLE_ENDIAN)

    @Synchronized
    fun writeFrame(frame: RequestFrame) {
        buffer.clear()

        buffer.putShort(frame.requestId)
        buffer.putShort(frame.messageType)
        buffer.putInt(frame.payloadData.size)
        buffer.put(frame.payloadData)

        buffer.flip()

        byteSink(buffer.readByteArray())
    }

}


internal class FrameInProgress {

    var requestId: Short? = null
        private set
    var resultCode: Short? = null
        private set
    var isComplete = false
        private set

    private var frameSize: Int? = null
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

        if (requestId == null) {
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
        require(isComplete, { "Cannot consume data, frame is not complete!" })

        ensureFrameIsNotReused()
        isConsumed = true

        return finalFrameData!!
    }

    private fun onFirstPacket() {
        requestId = packetBuffer.short
        resultCode = packetBuffer.short
        frameSize = packetBuffer.int
        require(frameSize!! >= 0, { "Invalid frame size: $frameSize" })
        frameDataBuffer.limit(frameSize!!)
    }

    private fun onComplete() {
        frameDataBuffer.flip()
        finalFrameData = frameDataBuffer.readByteArray(frameSize!!)
    }

    // Ensure that the remainder of a packet is all zeros
    // (this is a sanity check to make ensure we don't receive bytes where we shouldn't)
    private fun checkRemainderOfPacketIsZeros() {
        if (!QATool.isDebugBuild) {
            return
        }

        while (packetBuffer.hasRemaining()) {
            val nextByte = packetBuffer.readByte().toInt()
            require(nextByte == 0, {
                val b = nextByte.toByte().toHex()
                val remainder = packetBuffer.readByteArray().toHex()
                "Found non-zero bytes in the remainder of a packet: byte=$b, remainder=$remainder"
            })
        }
    }

    private fun ensureFrameIsNotReused() {
        if (isConsumed) {
            throw IllegalStateException("Frame already consumed!  Instances cannot be reused!")
        }
    }
}
