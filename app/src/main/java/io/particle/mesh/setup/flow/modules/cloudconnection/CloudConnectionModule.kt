package io.particle.mesh.setup.flow.modules.cloudconnection

import io.particle.android.sdk.cloud.ParticleCloud
import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging


class CloudConnectionModule(
        private val cloud: ParticleCloud
) {

    private val log = KotlinLogging.logger {}

    var claimCode: String? = null

    fun fetchClaimCode() {
        launch {
            if (claimCode != null) {
                log.debug { "Claim code already fetched; skipping" }
                return@launch
            }
            log.info { "Fetching new claim code" }
            claimCode = cloud.generateClaimCode().claimCode
        }
    }


}