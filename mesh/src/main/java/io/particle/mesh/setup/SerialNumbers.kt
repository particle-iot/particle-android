package io.particle.mesh.setup

import io.particle.mesh.setup.flow.SerialNumber


fun SerialNumber.isSomSerial(): Boolean {
    return value.toLowerCase().startsWith("p00")
}
