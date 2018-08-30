package io.particle.mesh.setup.flow.modules.cloudconnection

import android.support.annotation.WorkerThread
import io.particle.android.sdk.cloud.ParticleCloud
import mu.KotlinLogging


class CloudConnectionModule(
        private val cloud: ParticleCloud
) {

    private val log = KotlinLogging.logger {}

    var claimCode: String? = null

    @WorkerThread
    fun fetchClaimCode() {
        log.info { "Fetching new claim code" }
        claimCode = cloud.generateClaimCode().claimCode
    }

}
