package io.particle.mesh.setup.connection

import io.particle.mesh.setup.utils.readByteArray
import io.particle.mesh.setup.utils.readUint16LE
import java.nio.ByteBuffer
import java.nio.ByteOrder


class DeviceRequest(
        val requestId: Short,
        val messageType: Short,
        val payloadData: ByteArray
) {

    init {
        // ID "0" is reserved, shouldn't be used by a client
        require(requestId != 0.toShort())
    }
}


class DeviceResponse(
        val requestId: Short,
        val resultCode: Int,
        val payloadData: ByteArray
)


class RequestWriter(
        private val sink: (OutboundFrame) -> Unit
) {

    private val buffer = ByteBuffer.allocate(MAX_FRAME_SIZE).order(ByteOrder.LITTLE_ENDIAN)

    @Synchronized
    fun writeRequest(request: DeviceRequest) {
        buffer.clear()

        buffer.putShort(request.requestId)
        buffer.putShort(request.messageType)
        buffer.putShort(0)  // for the "reserved" field
        buffer.put(request.payloadData)

        buffer.flip()

        sink(OutboundFrame(
                buffer.readByteArray(),
                request.payloadData.size
        ))
    }

}


class ResponseReader(
        private val sink: (DeviceResponse) -> Unit
) {

    private val buffer = ByteBuffer.allocate(MAX_FRAME_SIZE).order(ByteOrder.LITTLE_ENDIAN)

    fun receiveResponseFrame(frame: InboundFrame) {
        buffer.clear()
        buffer.put(frame.frameData)
        buffer.flip()

        val requestId = buffer.readUint16LE().toShort()
        val result = buffer.int
        val payload = buffer.readByteArray()

        val response = DeviceResponse(requestId, result, payload)
        sink(response)
    }

}