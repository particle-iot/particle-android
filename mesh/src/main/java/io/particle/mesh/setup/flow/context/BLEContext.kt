package io.particle.mesh.setup.flow.context

import io.particle.mesh.common.logged
import io.particle.mesh.setup.flow.Clearable
import mu.KotlinLogging


class BLEContext : Clearable {

    private val log = KotlinLogging.logger {}

    var targetDevice = SetupDevice(DeviceRole.SETUP_TARGET)
    var commissioner = SetupDevice(DeviceRole.COMMISSIONER)

    var connectingToTargetUiShown by log.logged(false)

    var connectingToAssistingDeviceUiShown by log.logged(false)
    var shownAssistingDeviceInitialIsConnectedScreen by log.logged(false)


    override fun clearState() {
        connectingToAssistingDeviceUiShown = false
        shownAssistingDeviceInitialIsConnectedScreen = false
        connectingToTargetUiShown = false

        targetDevice.transceiverLD.value?.disconnect()
        commissioner.transceiverLD.value?.disconnect()

        targetDevice = SetupDevice(DeviceRole.SETUP_TARGET)
        commissioner = SetupDevice(DeviceRole.COMMISSIONER)
    }

}