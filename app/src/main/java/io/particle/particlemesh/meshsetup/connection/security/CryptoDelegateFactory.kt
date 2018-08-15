package io.particle.particlemesh.meshsetup.connection.security

import io.particle.ecjpake4j.ECJPakeImpl
import io.particle.ecjpake4j.Role
import io.particle.ecjpake4j.Role.CLIENT
import io.particle.particlemesh.bluetooth.PacketMTUSplitter
import io.particle.particlemesh.bluetooth.connecting.BTDeviceAddress
import io.particle.particlemesh.bluetooth.connecting.MeshSetupConnection
import io.particle.particlemesh.bluetooth.connecting.MeshSetupConnectionFactory
import io.particle.particlemesh.bluetooth.packetTxRxContext
import io.particle.particlemesh.common.QATool
import io.particle.particlemesh.meshsetup.connection.*
import kotlinx.coroutines.experimental.launch


class CryptoDelegateFactory {

    suspend fun createCryptoDelegate(
            meshSetupConnection: MeshSetupConnection,
            frameWriter: OutboundFrameWriter,
            frameReader: InboundFrameReader,
            jpakeLowEntropyPassword: String
    ): AesCcmDelegate? {

        val transceiver = JpakeExchangeMessageTransceiver(frameWriter, frameReader)
        val jpakeManager = JpakeExchangeManager(
                ECJPakeImpl(Role.CLIENT, jpakeLowEntropyPassword),
                transceiver
        )

//        TODO("this will break.  need the broadcast channel after all?")
        val jpakeMgrChannel = meshSetupConnection.packetReceiveChannel
        launch(packetTxRxContext) {
            for (packet in jpakeMgrChannel) {
                QATool.runSafely({ frameReader.receivePacket(BlePacket(packet)) })
            }
        }

        val jpakeSecret = jpakeManager.performJpakeExchange()
        jpakeMgrChannel.cancel()  // stop reading on this channel, it's no longer going to be used

        return AesCcmDelegate.newDelegateFromJpakeSecret(jpakeSecret)
    }

}