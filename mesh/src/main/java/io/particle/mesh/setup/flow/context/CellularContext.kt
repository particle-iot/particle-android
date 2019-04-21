package io.particle.mesh.setup.flow.context

import io.particle.mesh.common.logged
import io.particle.mesh.setup.flow.Clearable
import mu.KotlinLogging


class CellularContext : Clearable {

    private val log = KotlinLogging.logger {}

    var connectingToCloudUiShown by log.logged(false)

    override fun clearState() {
        log.info { "clearState()" }
        connectingToCloudUiShown = false
    }

}