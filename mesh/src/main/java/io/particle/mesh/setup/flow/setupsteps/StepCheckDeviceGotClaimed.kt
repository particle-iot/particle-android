package io.particle.mesh.setup.flow.setupsteps

import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.mesh.setup.flow.DeviceConnectToCloudTimeoutException
import io.particle.mesh.setup.flow.FlowException
import io.particle.mesh.setup.flow.MeshSetupStep
import io.particle.mesh.setup.flow.Scopes
import io.particle.mesh.setup.flow.context.SetupContexts
import kotlinx.coroutines.delay
import mu.KotlinLogging


class StepCheckDeviceGotClaimed(private val cloud: ParticleCloud) : MeshSetupStep() {

    private val log = KotlinLogging.logger {}

    override suspend fun doRunStep(ctxs: SetupContexts, scopes: Scopes) {
        if (ctxs.ble.targetDevice.isClaimedLD.value == true) {
            return
        }

        suspend fun pollDevicesForNewDevice(deviceId: String): Boolean {
            val idLower = deviceId.toLowerCase()

            var millis = 0L
            val limitMillis = 45 * 1000 // 45 seconds
            while (millis < limitMillis) {
                // FIXME: what should the timing be here?
                val delay = 2000L
                delay(delay)
                millis += delay
                val userOwnsDevice = try {
                    cloud.userOwnsDevice(idLower)
                } catch (ex: Exception) {
                    false
                }
                if (userOwnsDevice) {
                    log.info { "Found assigned to user device with ID $deviceId" }
                    return true
                }
                log.info { "No device with ID $deviceId found yet assigned to user" }
            }

            log.warn { "Timed out waiting for user to own a device with ID $deviceId" }
            return false
        }

        val isInList = pollDevicesForNewDevice(ctxs.ble.targetDevice.deviceId!!)
        if (!isInList) {
            throw DeviceConnectToCloudTimeoutException()
        }

        val device = cloud.getDevice(ctxs.ble.targetDevice.deviceId!!)

        ctxs.ble.targetDevice.currentDeviceName = device.name

        ctxs.ble.targetDevice.updateIsClaimed(true)
    }

}