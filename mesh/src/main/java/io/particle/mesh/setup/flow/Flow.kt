package io.particle.mesh.setup.flow

import io.particle.mesh.common.Result
import mu.KotlinLogging


// FIXME: put these somewhere better


enum class Gen3ConnectivityType {
    WIFI,
    CELLULAR,
    MESH_ONLY
}

private val log = KotlinLogging.logger {}


fun <V, E> Result<V, E>.throwOnErrorOrAbsent(): V {
    return when (this) {
        is Result.Error,
        is Result.Absent -> {
            val msg = "Error making request: ${this.error}"
            log.error { msg }
            throw FlowException(msg)
        }
        is Result.Present -> this.value
    }
}
