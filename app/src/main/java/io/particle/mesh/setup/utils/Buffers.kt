package io.particle.mesh.setup.utils

import okio.Buffer


fun Buffer.readUint16LE(): Int {
    return ByteMath.readUint16LE(this)
}

fun Buffer.writeUint16LE(value: Int): Buffer {
    ByteMath.writeUint16LE(this, value)
    return this
}

