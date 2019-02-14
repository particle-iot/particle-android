package io.particle.mesh.bluetooth

import io.particle.mesh.setup.utils.readByteArray
import java.nio.ByteBuffer


class PacketMTUSplitter(
        private val outputTarget: (ByteArray) -> Unit,
        private val mtuSize: Int = 20,
        bufferSize: Int = 10240
) {

    private val buffer = ByteBuffer.allocate(bufferSize)

    @Synchronized
    fun splitIntoPackets(bytes: ByteArray) {
        buffer.clear()
        buffer.put(bytes)
        buffer.flip()

        while (buffer.hasRemaining()) {
            val packet = if (buffer.remaining() >= mtuSize) {
                buffer.readByteArray(mtuSize)
            } else {
                buffer.readByteArray()
            }
            outputTarget(packet)
        }
    }

}