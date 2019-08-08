package io.particle.mesh.setup.connection


import androidx.annotation.MainThread
import io.particle.mesh.bluetooth.PacketMTUSplitter
import io.particle.mesh.bluetooth.connecting.BluetoothConnection
import io.particle.mesh.common.QATool
import io.particle.mesh.setup.connection.security.SecurityManager
import io.particle.mesh.setup.flow.Scopes
import mu.KotlinLogging


private const val FULL_PROTOCOL_HEADER_SIZE = 6
private const val AES_CCM_MAC_SIZE = 8


class ProtocolTransceiverFactory(
        private val securityManager: SecurityManager
) {

    private val log = KotlinLogging.logger {}

    @MainThread
    suspend fun buildProtocolTransceiver(
        deviceConnection: BluetoothConnection,
        name: String,
        connectionScopes: Scopes,
        jpakeLowEntropyPassword: String,
        setupContextScopes: Scopes
    ): ProtocolTransceiver? {

        val packetMTUSplitter = PacketMTUSplitter({ packet ->
            deviceConnection.packetSendChannel.offer(packet)
        })
        val frameWriter = OutboundFrameWriter { packetMTUSplitter.splitIntoPackets(it) }
        val frameReader = InboundFrameReader(connectionScopes)

        connectionScopes.onWorker {
            for (packet in deviceConnection.packetReceiveChannel) {
                QATool.runSafely({ frameReader.receivePacket(BlePacket(packet)) })
            }
        }

        log.info { "Creating connection crypto delegate" }

        val cryptoDelegate = securityManager.createCryptoDelegate(
                jpakeLowEntropyPassword,
                frameWriter,
                frameReader
        ) ?: return null

        frameReader.cryptoDelegate = cryptoDelegate
        frameWriter.cryptoDelegate = cryptoDelegate

        // now that we've passed the security handshake, set the correct number of bytes to assume
        // for the message headers
        frameReader.extraHeaderBytes = FULL_PROTOCOL_HEADER_SIZE + AES_CCM_MAC_SIZE

        val requestWriter = RequestWriter { frameWriter.writeFrame(it) }
        val requestSender = ProtocolTransceiver(
            requestWriter,
            deviceConnection,
            connectionScopes,
            name,
            setupContextScopes
        )
        val responseReader = ResponseReader { requestSender.receiveResponse(it) }
        connectionScopes.onWorker  {
            for (inboundFrame in frameReader.inboundFrameChannel) {
                QATool.runSafely({ responseReader.receiveResponseFrame(inboundFrame) })
            }
        }

        return requestSender
    }

}
