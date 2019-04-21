package io.particle.mesh.setup.flow.context

import io.particle.mesh.common.logged
import io.particle.mesh.setup.flow.Clearable
import mu.KotlinLogging


class BLEContext : Clearable {

    private val log = KotlinLogging.logger {}

    var connectingToTargetUiShown by log.logged(false)

    var connectingToAssistingDeviceUiShown by log.logged(false)
    var shownAssistingDeviceInitialIsConnectedScreen by log.logged(false)


    override fun clearState() {
        connectingToAssistingDeviceUiShown = false
        shownAssistingDeviceInitialIsConnectedScreen = false
        connectingToTargetUiShown = false
    }

}