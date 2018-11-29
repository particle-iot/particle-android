package io.particle.mesh.setup.connection

import java.util.concurrent.TimeUnit


// FIXME: set this to something smaller, or rely on DeviceOS's timeouts?
// this has to be LONG for some of the messages to be processed (like AuthRequest)
val BLE_PROTO_REQUEST_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(125).toLong()
