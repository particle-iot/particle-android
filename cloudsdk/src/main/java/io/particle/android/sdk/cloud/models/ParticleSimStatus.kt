package io.particle.android.sdk.cloud.models


enum class ParticleSimStatus {
    READY_TO_ACTIVATE,  // ok == "ready"
    ACTIVATED_FREE,     // already activated on a free plan
    ACTIVATED,          // already activated
    NOT_FOUND,
    NOT_OWNED_BY_USER,
    ERROR
}


internal fun statusCodeToSimStatus(statusCode: Int): Pair<ParticleSimStatus, String> {
    when (statusCode) {
        204 -> return Pair(ParticleSimStatus.ACTIVATED_FREE, "SIM card is activated and on a free plan")
        205 -> return Pair(ParticleSimStatus.ACTIVATED, "SIM card is already activated")
        403 -> return Pair(ParticleSimStatus.NOT_OWNED_BY_USER, "SIM card is owned by another user")
        404 -> return Pair(ParticleSimStatus.NOT_FOUND, "SIM card not found")
    }

    return if (statusCode in 200..299) {
        Pair(ParticleSimStatus.READY_TO_ACTIVATE, "SIM card is ready")
    } else {
        Pair(ParticleSimStatus.ERROR, "SIM card check error")
    }
}