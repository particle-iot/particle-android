package io.particle.mesh.setup



inline class SerialNumber(val value: String)


fun SerialNumber.isSomSerial(): Boolean {
    return value.toLowerCase().startsWith("p00")
}
