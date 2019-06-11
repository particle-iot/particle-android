package io.particle.mesh.setup.flow

import io.particle.mesh.common.Result
import mu.KotlinLogging


// FIXME: put these somewhere better


enum class Gen3ConnectivityType {
    WIFI,
    CELLULAR,
    MESH_ONLY
}


inline fun <reified V, reified E> Result<V, E>.throwOnErrorOrAbsent(): V {
    return when (this) {
        is Result.Error,
        is Result.Absent -> {
            val log = KotlinLogging.logger {}
            val msg = if (this is Result.Error) {
                "Error getting result: ${this.error}"
            } else {
                "Absent result returned! value type=${V::class}, error type=${E::class}"
            }
            log.error { msg }
            throw MeshSetupFlowException(null, msg)
        }
        is Result.Present -> this.value
    }
}
