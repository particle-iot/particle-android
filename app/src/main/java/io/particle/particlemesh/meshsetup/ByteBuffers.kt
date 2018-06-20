package io.particle.particlemesh.meshsetup

import java.nio.ByteBuffer


/**
 * Extensions to make ByteBuffer's API less incredi-bad/more like Okio's Buffer
 */


fun ByteBuffer.readByte(): Byte = this.get()


fun ByteBuffer.putUntilFull(other: ByteBuffer) {
    // there's method call overhead here, but given the context, I'm not too concerned.
    while (this.hasRemaining() && other.hasRemaining()) {
        this.put(other.get())
    }
}


/**
 * Read all remaining bytes in the buffer to an array
 */
fun ByteBuffer.readByteArray(): ByteArray {
    val copyTarget = ByteArray(this.remaining())
    this.get(copyTarget, 0, copyTarget.size)
    return copyTarget
}


fun ByteBuffer.readByteArray(bytesToRead: Int): ByteArray {
    val copyTarget = ByteArray(bytesToRead)
    this.get(copyTarget, 0, copyTarget.size)
    return copyTarget
}


fun ByteBuffer.snapshot(): ByteArray {
    val dupe = this.duplicate()
    dupe.flip()
    return dupe.readByteArray()
}